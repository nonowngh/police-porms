package mb.fw.atb.strategy.pubsub;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTrace;
import mb.fw.atb.config.FieldsSchema;
import mb.fw.atb.config.Specifications;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.enums.THeader;
import mb.fw.atb.error.ATBException;
import mb.fw.atb.error.ErrorCode;
import mb.fw.atb.job.file.DirectoryPolling;
import mb.fw.atb.job.file.IFTPJob;
import mb.fw.atb.job.file.TempMove;
import mb.fw.atb.model.OnSignalInfo;
import mb.fw.atb.model.file.FileMessage;
import mb.fw.atb.schema.pubsub.StandardDFTFDStrategySchema;
import mb.fw.atb.service.DBTransactionService;
import net.sf.flatpack.DataSet;
import net.sf.flatpack.Parser;
import net.sf.flatpack.brparse.BuffReaderParseFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component(value = "StandardDFTFDStrategy")
@Slf4j
public class StandardDFTFDStrategy extends StandardFTFDStrategy {

    @Autowired(required = false)
    @Qualifier("DBTransactionService")
    DBTransactionService dbTransactionService;

    @Autowired
    IFTPJob iftpJob;

    @Autowired
    TempMove tempMove;

    @Override
    public Specifications specifications() {
        return StandardDFTFDStrategySchema.specifications();
    }

    @Override
    @TimeTrace
    public OnSignalInfo onSignal(IFContext context, String txid, String msgCreDt) throws Exception {
        Map contextMap = context.createContextMap();
        contextMap.put(THeader.SENDER_MSG_CREATE_DT.key(), msgCreDt);
        contextMap.put(THeader.TRANSACTION_ID.key(), txid);

        log.debug("IFContext parameter Map {}", contextMap);

        Path sendPath = null;
        Path errorPath = null;
        Path tempPath = null;
        Path complete = null;

        try {
            String fileSendPath = context.getFileSendPath();
            String fileErrorPath = context.getFileErrorPath();
            String fileTempPath = context.getFileTempPath();
            String fileCompletePath = context.getFileCompletePath();

            sendPath = Paths.get(fileSendPath);
            errorPath = Paths.get(fileErrorPath);
            tempPath = Paths.get(fileTempPath);
            complete = Paths.get(fileCompletePath);

            log.info("SEND_PATH ==> {}", sendPath);
            DirectoryPolling.checkAndCreateDirectories(errorPath);
            DirectoryPolling.checkAndCreateDirectories(tempPath);
            DirectoryPolling.checkAndCreateDirectories(complete);

        } catch (Exception e) {
            String errorMsg = "[" + txid + "] FILE " + context.getInterfaceId() + " DIRECTORIES CHECK ERROR";
            log.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }

        /**
         * 대용량 매커니즘 변경 transaction ID 의 상태코드를  update하기위함
         */
        //DB에서 연계 상태코드를 'P'로 변경하고 TRANSACTION_ID 를 업데이트 한다.
        int txSize = dbTransactionService.update(context.getInterfaceId() + ".TX_UPDATE", context, contextMap, Maps.newLinkedHashMap());

        log.info("TX_UPDATE SIZE {}", txSize);
        if (txSize == 0) {
            log.info("NO DATA FOUND SCHEDULE STOPPED ");
            return null;
        }

        Path selectFile = null;

        try {
            //단일 테이블 이나 Master 테이블 데이터 연계파일은 확장자명이 data 로 저장된다.
            selectFile = dbTransactionService.selectToFile(context.getInterfaceId() + ".SELECT", context, contextMap, "data", false);
        } catch (Exception e) {
            log.error("DB SELECT_TO_GENERATE_FILE FAILURE", e);
            ErrorCode errorCode = ErrorCode.SELECT_FAILURE;
            ATBException atbException = new ATBException("DB SELECT_TO_GENERATE_FILE", errorCode, e.getCause());
            log.error((errorCode.getStatus() + "," + errorCode.getCode() + "," + errorCode.getMessage()), atbException.getCause());
            throw atbException;
        }
        int size = Integer.parseInt((String) contextMap.get(THeader.SENDER_DATA_COUNT.key()));

        log.info("SELECT SIZE {}", size);
        if (selectFile == null || size == 0) {
            log.info("NO DATA FOUND SCHEDULE STOPPED ");
            return null;
        }

        Set<Path> transferSet = new LinkedHashSet();

        FileMessage fileMessage = new FileMessage();

        //child data 처리
        if (context.isDetailData()) {
            detailExtract(context, selectFile, contextMap, transferSet);
            fileMessage.setDetailData(true);
        }

        try {
            List<Path> pollList = Lists.newArrayList();
            pollList.add(selectFile);
            if (context.isDetailData()) {
                pollList.addAll(transferSet);
            }

            List<Path> tempList = null;

            tempList = tempMove.tempMove(pollList, context, txid);

            List<String> fileNameList = null;
            log.info("IFTPClient {}", client);
            fileNameList = iftpJob.putNRollback(client, tempList, context, txid);


            generateFileMessage(context, txid, fileNameList, fileMessage);

            return OnSignalInfo.builder().count(size).sendObject(fileMessage).build();
        } catch (Throwable t) {
            log.error("ATB Schedule Error", t);
            throw t;
        }

    }

    /**
     * Detail Table을 조회하여 파일에 쌓는다.
     *
     * @param context
     * @param selectFile
     * @param contextMap
     * @param transferSet
     * @throws FileNotFoundException
     */
    @TimeTrace
    private void detailExtract(IFContext context, Path selectFile, Map contextMap, Set<Path> transferSet) throws IOException {
        boolean isFirstHeader = true;
        Parser parser = null;
        //selectFile(Master 파일)을 다시 열어서 Detail Table를 조회하여 파일에 쌓는다.
        try (FileReader fileReader = new FileReader(selectFile.toFile())) {
            parser = BuffReaderParseFactory.getInstance().newDelimitedParser(fileReader, context.getFileDelimiter(), context.getFileDelimiterQualifier());
            DataSet ds = parser.parse();

            while (ds.next()) {
                Map masterRecord = Maps.newLinkedHashMap();
                for (String column : ds.getColumns()) {
                    masterRecord.put(column, ds.getString(column));
                }
                String[] childNames = context.getDetailNames();
                masterRecord.putAll(contextMap);
                for (String childName : childNames) {
                    //Detail 테이블은 확장자명이 childName 으로 저장된다.  append
                    Path childFile = dbTransactionService.selectToFileNtime(context.getInterfaceId() + ".SELECT_" + childName, context, masterRecord, childName, true);
                    transferSet.add(childFile);
                }
            }
        }
    }


    @Override
    @TimeTrace
    public OnSignalInfo onSignalRetry(IFContext context, String txid, String
            msgCreDt, Map<String, String> propMap) throws Exception {
        Map contextMap = context.createContextMap();
        contextMap.put(THeader.SENDER_MSG_CREATE_DT.key(), msgCreDt);
        contextMap.put(THeader.TRANSACTION_ID.key(), txid);

        contextMap.putAll(propMap);

        log.debug("IFContext parameter Map {}", contextMap);

        Path sendPath = null;
        Path errorPath = null;
        Path tempPath = null;
        Path complete = null;

        try {


            String fileSendPath = context.getFileSendPath();
            String fileErrorPath = context.getFileErrorPath();
            String fileTempPath = context.getFileTempPath();
            String fileCompletePath = context.getFileCompletePath();

            sendPath = Paths.get(fileSendPath);
            errorPath = Paths.get(fileErrorPath);
            tempPath = Paths.get(fileTempPath);
            complete = Paths.get(fileCompletePath);

            log.info("SEND_PATH ==> {}", sendPath);
            DirectoryPolling.checkAndCreateDirectories(errorPath);
            DirectoryPolling.checkAndCreateDirectories(tempPath);
            DirectoryPolling.checkAndCreateDirectories(complete);

        } catch (Exception e) {
            String errorMsg = "[" + txid + "] FILE " + context.getInterfaceId() + " DIRECTORIES CHECK ERROR";
            log.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }

        Path selectFile = null;

        try {
            selectFile = dbTransactionService.selectToFile(context.getInterfaceId() + ".SELECT_RESEND", context, contextMap, "data", false);
        } catch (Exception e) {
            log.error("DB SELECT_TO_GENERATE_FILE FAILURE", e);
            ErrorCode errorCode = ErrorCode.SELECT_FAILURE;
            ATBException atbException = new ATBException("DB SELECT_TO_GENERATE_FILE", errorCode, e.getCause());
            log.error((errorCode.getStatus() + "," + errorCode.getCode() + "," + errorCode.getMessage()), atbException.getCause());
            throw atbException;
        }
        int size = Integer.parseInt((String) contextMap.get(THeader.SENDER_DATA_COUNT.key()));

        log.info("SELECT SIZE {}", size);
        if (selectFile == null || size == 0) {
            log.info("NO DATA FOUND SCHEDULE STOPPED ");
            return null;
        }

        Set<Path> transferSet = new LinkedHashSet();

        FileMessage fileMessage = new FileMessage();

        //child data 처리
        if (context.isDetailData()) {
            detailExtract(context, selectFile, contextMap, transferSet);
            fileMessage.setDetailData(true);
        }

        try {
            List<Path> pollList = Lists.newArrayList();
            pollList.add(selectFile);
            if (context.isDetailData()) {
                pollList.addAll(transferSet);
            }

            List<Path> tempList = null;

            tempList = tempMove.tempMove(pollList, context, txid);

            List<String> fileNameList = null;
            log.info("IFTPClient {}", client);
            fileNameList = iftpJob.putNRollback(client, tempList, context, txid);

            generateFileMessage(context, txid, fileNameList, fileMessage);

            return OnSignalInfo.builder().count(fileMessage.getCount()).sendObject(fileMessage).build();
        } catch (Throwable t) {
            log.error("ATB Schedule Error", t);
            throw t;
        }
    }

    @Override
    @TimeTrace
    public void onMessageResult(IFContext context, String txid, String eventDt, String resultCode, String resultMessage, String dataStr, Map<String, String> propMap) throws Exception {
        //file to file Strategy 결과 처리후
        super.onMessageResult(context, txid, eventDt, resultCode, resultMessage, dataStr, propMap);
        //DB에 결과를 업데이트 한다.
        dbTransactionService.update(context.getInterfaceId() + ".RESULT_UPDATE", context, propMap, Maps.newLinkedHashMap());
    }
}
