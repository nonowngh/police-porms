package mb.fw.atb.strategy.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTrace;
import mb.fw.atb.aop.TimeTraceAspect;
import mb.fw.atb.config.Specifications;
import mb.fw.atb.enums.FileType;
import mb.fw.atb.enums.THeader;
import mb.fw.atb.enums.TResult;
import mb.fw.atb.error.ATBException;
import mb.fw.atb.error.ErrorCode;
import mb.fw.atb.job.file.*;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.model.OnSignalInfo;
import mb.fw.atb.model.file.FileDataError;
import mb.fw.atb.model.file.FileInfo;
import mb.fw.atb.model.file.FileMessage;
import mb.fw.atb.schema.batch.StandardFTDBatchSchema;
import mb.fw.atb.service.DBTransactionService;
import mb.fw.atb.strategy.ATBStrategy;
import mb.fw.atb.util.ATBUtil;
import mb.fw.atb.util.DateUtils;
import net.sf.flatpack.DataError;
import net.sf.flatpack.DataSet;
import net.sf.flatpack.Parser;
import net.sf.flatpack.brparse.BuffReaderParseFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component(value = "StandardFTDBatch")
@Slf4j
public class StandardFTDBatch extends ATBStrategy {

    @Autowired(required = false)
    @Qualifier("DBTransactionService")
    DBTransactionService dbTransactionService;

    @Autowired
    DirectoryPolling directoryPolling;

    @Autowired
    IFTPJob iftpJob;

    @Autowired
    FileRecvJob fileRecvJob;

    @Autowired
    TempMove tempMove;

    ObjectMapper mapper = new ObjectMapper();

    @Override
    public Specifications specifications() {
        return StandardFTDBatchSchema.specifications();
    }

    /**
     * example :
     *
     * @param context
     * @param txid
     * @param msgCreDt
     * @throws Exception
     */
    @Override
    @TimeTrace
    public OnSignalInfo onSignal(IFContext context, String txid, String msgCreDt) throws Exception {
        List<Path> pollList = directoryPolling.fileSendPoll(txid, context);

        if (pollList.size() == 0) {
            log.info("SCHEDULE STOPPED");
            return null;
        }

        int count = pollList.size();

        try {
            String sendDt = DateUtils.today17();
            ATBUtil.startLogging(jmsTemplate, context.getInterfaceId(), txid, new String[]{config.getId()}, count, context.getSendSystemCode(), context.getReceiveSystemCode(), msgCreDt, sendDt);
        } catch (Exception e) {
            ErrorCode errorCode = ErrorCode.JMS_SEND;
            ATBException atbException = new ATBException("JMS SEND FAILURE", errorCode, e.getCause());
           log.error((errorCode.getStatus() + "," + errorCode.getCode() + "," + errorCode.getMessage()), atbException.getCause());
            throw atbException;
        }
        List<Path> tempList = null;

        tempList = tempMove.tempMove(pollList, context, txid);

        FileMessage fileMessage = processFileToDB(context, txid, msgCreDt, tempList, false);

        return OnSignalInfo.builder().count(fileMessage.getCount()).processEnd(true).sendObject(fileMessage).build();
    }

    @NotNull
    private FileMessage processFileToDB(IFContext context, String txid, String msgCreDt, List<Path> tempList, boolean resendYn) throws Exception {
        FileMessage fileMessage = new FileMessage();

        generateFileMessage(context, txid, tempList, fileMessage);

        List<FileInfo> retFileInfoList = Lists.newArrayList();

        FileMessage retFileMessage = new FileMessage();
        Map propMap = Maps.newLinkedHashMap();
        propMap.put(THeader.INTERFACE_ID.key(), context.getInterfaceId());
        propMap.put(THeader.TRANSACTION_ID.key(), txid);
        propMap.put(THeader.SENDER_ID.key(), config.getId());
        propMap.put(THeader.SENDER_MSG_CREATE_DT.key(), msgCreDt);
        propMap.put(THeader.SENDER_STRATEGY.key(), context.getActionType());
        propMap.put(THeader.SENDER_MSG_SEND_DT.key(), DateUtils.today17());
        propMap.put(THeader.SENDER_ADAPTOR_NAME.key(), adaptorConfigBean.getAdaptorName());
        propMap.put(THeader.SENDER_DATA_COUNT.key(), String.valueOf(tempList.size()));
        propMap.put(THeader.RECEIVER_ID.key(), adaptorConfigBean.getAdaptorName());
        propMap.put(THeader.RESEND_YN.key(), resendYn ? "Y" : "N");

        try {
            //저장된걸 csv형태의 파일이라고 생각하고 DB에 입력한다.
            switch (FileType.valueOf(context.getFileType())) {
                case DELIMITER: {
                    boolean isFirstHeader = context.isFileFirstHeader();

                    for (Path insertFile : tempList) {

                        Parser parser = null;
                        FileReader insertReader = new FileReader(insertFile.toFile());

                        if (isFirstHeader) {
                            parser = BuffReaderParseFactory.getInstance().newDelimitedParser(insertReader, context.getFileDelimiter(), context.getFileDelimiterQualifier());
                        } else {
                            File pzmap = new ClassPathResource(context.getInterfaceId() + ".pzmap").getFile();
                            FileReader pzmapReader = new FileReader(pzmap);
                            try{
                                parser = BuffReaderParseFactory.getInstance().newDelimitedParser(pzmapReader, insertReader, context.getFileDelimiter(), context.getFileDelimiterQualifier(), false);
                            }finally {
                                pzmapReader.close();
                            }
                        }

                        DataSet ds = parser.parse();

                        try {
                            dbTransactionService.insertFlatpack(ds, context.getInterfaceId() + ".INSERT", context, propMap, context.isFileErrorSkip(), insertFile.getFileName().toString());
                        } finally {

                            insertReader.close();

                            if (ds.getErrors() != null && !ds.getErrors().isEmpty()) {
                                retFileMessage.setErrorCount(1);
                            }
                            FileInfo fileInfo = new FileInfo();
                            fileInfo.setFileName(insertFile.getFileName().toString());
                            fileInfo.setSize(Files.size(insertFile));
                            fileInfo.setParseErrorCount(ds.getErrorCount());
                            List<FileDataError> errorList = Lists.newArrayList();

                            for (DataError de : ds.getErrors()) {
                                FileDataError dataError = new FileDataError();
                                dataError.parseDataError(de);
                                errorList.add(dataError);
                            }
                            fileInfo.setErrors(errorList);

                            retFileInfoList.add(fileInfo);
                        }

                        if (context.isProcedureCall()) {
                            dbTransactionService.call(context.getInterfaceId() + ".CALL", context, propMap);
                        }
                    }
                    break;
                }
                case FIXEDLENGTH: {
                    for (Path insertFile : tempList) {

                        File pzmap = new ClassPathResource(context.getInterfaceId() + ".pzmap").getFile();
                        FileReader pzmapReader = new FileReader(pzmap);
                        FileReader insertReader = new FileReader(insertFile.toFile());
                        final Parser parser = BuffReaderParseFactory.getInstance().newFixedLengthParser(pzmapReader, insertReader);
                        DataSet ds = parser.parse();

                        try {
                            dbTransactionService.insertFlatpack(ds, context.getInterfaceId() + ".INSERT", context, propMap, context.isFileErrorSkip(), insertFile.getFileName().toString());
                        } finally {
                            if (ds.getErrors() != null && !ds.getErrors().isEmpty()) {
                                retFileMessage.setErrorCount(1);
                            }
                            FileInfo fileInfo = new FileInfo();
                            fileInfo.setFileName(insertFile.getFileName().toString());
                            fileInfo.setSize(Files.size(insertFile));
                            fileInfo.setParseErrorCount(ds.getErrorCount());
                            List<FileDataError> errorList = Lists.newArrayList();
                            for (DataError de : ds.getErrors()) {
                                FileDataError dataError = new FileDataError();
                                dataError.parseDataError(de);
                                errorList.add(dataError);
                            }
                            fileInfo.setErrors(errorList);

                            retFileInfoList.add(fileInfo);
                        }
                        if (context.isProcedureCall()) {
                            dbTransactionService.call(context.getInterfaceId() + ".CALL", context, propMap);
                        }
                    }
                    break;
                }
                default: {
                    log.info("UNSUPPORTED FILE TYPE");
                    throw new RuntimeException("UNSUPPORTED FILE TYPE");
                }
            }
            //정상일경우 success로 실패일경우 fail로 이동
            fileRecvJob.resultProcessFileInfo(txid, TResult.SUCCESS.value(), "OK", context, fileMessage.getFileInfoList());

            String msgRcvDt = DateUtils.today17();
            propMap.put(THeader.RECEIVER_MSG_RECV_DT.key(), msgRcvDt);
            propMap.put(THeader.RECEIVER_ADAPTOR_NAME.key(), adaptorConfigBean.getAdaptorName());
            propMap.put(THeader.RECEIVER_RESULT_CD.key(), TResult.SUCCESS.value());
            propMap.put(THeader.RECEIVER_RESULT_MSG.key(), "OK");

            String sendMessageJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(fileMessage);
            String propertyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(propMap);

            String timeTraceStr = "### TIME_TRACE\n" + TimeTraceAspect.generateTimeTraceAndRemove(txid);

            if (!resendYn) {
                ATBUtil.endLogging(jmsTemplate, context.getInterfaceId(), txid, config.getId(), 0, (String) propMap.get(THeader.RECEIVER_RESULT_CD.key()), "### RESULT\n" + sendMessageJson + "\n\n### PROPERTY\n" + propertyJson + "\n\n" + timeTraceStr, msgRcvDt);
            } else {
                ATBUtil.reEndLogging(jmsTemplate, context.getInterfaceId(), txid, config.getId(), 0, (String) propMap.get(THeader.RECEIVER_RESULT_CD.key()), "### RESULT\n" + sendMessageJson + "\n\n### PROPERTY\n" + propertyJson + "\n\n" + timeTraceStr, msgRcvDt);
            }

        } catch (Exception e) {
            //정상일경우 success로 실패일경우 fail로 이동
            fileRecvJob.resultProcessFileInfo(txid, TResult.FAIL.value(), e.getMessage(), context, fileMessage.getFileInfoList());
            String msgRcvDt = DateUtils.today17();
            propMap.put(THeader.RECEIVER_MSG_RECV_DT.key(), msgRcvDt);
            propMap.put(THeader.RECEIVER_ADAPTOR_NAME.key(), adaptorConfigBean.getAdaptorName());
            propMap.put(THeader.RECEIVER_RESULT_CD.key(), TResult.FAIL.value());
            propMap.put(THeader.RECEIVER_RESULT_MSG.key(), e.getMessage());

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = "###ERROR\n" + sw.toString();

            String propertyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(propMap);
            if (!resendYn) {
                ATBUtil.endLogging(jmsTemplate, context.getInterfaceId(), txid, config.getId(), retFileInfoList.size(), (String) propMap.get(THeader.RECEIVER_RESULT_CD.key()), "\n\n### PROPERTY\n" + propertyJson + "\n\n" + exceptionAsString, msgRcvDt);
            } else {
                ATBUtil.reEndLogging(jmsTemplate, context.getInterfaceId(), txid, config.getId(), retFileInfoList.size(), (String) propMap.get(THeader.RECEIVER_RESULT_CD.key()), "\n\n### PROPERTY\n" + propertyJson + "\n\n" + exceptionAsString, msgRcvDt);
            }
            throw e;
        } finally {
            retFileMessage.setFileInfoList(retFileInfoList);
            retFileMessage.setCount(retFileInfoList.size());
        }
        return fileMessage;
    }


    @Override
    @TimeTrace
    public OnSignalInfo onSignalRetry(IFContext context, String txid, String eventDt, Map<String, String> propMap) throws Exception {
        log.info("ONSIGNAL_RETRY STARTED ==> {}", txid);

        //temp , complete , error 디렉토리에에 있는 파일을 찾아서 전송해야한다.
        String fileErrorPath = context.getFileErrorPath();
        String fileTempPath = context.getFileTempPath();
        String fileCompletePath = context.getFileCompletePath();
        String fileSuccessPath = context.getFileSuccessPath();

        Path retryTempPath = Paths.get(fileTempPath + IFTPJob.separator + txid);
        Path retryCompletePath = Paths.get(fileCompletePath + IFTPJob.separator + txid);
        Path retrySuccessPath = Paths.get(fileSuccessPath + IFTPJob.separator + txid);

        boolean isFindFile = false;

        Path retryErrorPath = Paths.get(fileErrorPath + IFTPJob.separator + txid);
        //에러에 있다면 안꼬이게 절차대로 temp로 옮기자
        List<Path> pollList = new ArrayList<Path>();
        List<Path> finalTempList = new ArrayList<Path>();

        if (Files.exists(retryErrorPath)) {
            log.info("재전송할 파일을 찾음 ==> {}", retryErrorPath);
            Files.list(retryErrorPath).forEach(path -> pollList.add(path));
            List<Path> tempList = tempMove.tempMove(pollList, context, txid);
            Files.delete(retryErrorPath);
            isFindFile = true;
            finalTempList.addAll(tempList);
        }

        if (!isFindFile && Files.exists(retryCompletePath)) {
            log.info("재전송할 파일을 찾음 ==> {}", retryCompletePath);
            Files.list(retryCompletePath).forEach(path -> pollList.add(path));
            List<Path> tempList = tempMove.tempMove(pollList, context, txid);
            Files.delete(retryCompletePath);
            isFindFile = true;
            finalTempList.addAll(tempList);
        }

        if (!isFindFile && Files.exists(retrySuccessPath)) {
            log.info("재전송할 파일을 찾음 ==> {}", retrySuccessPath);
            Files.list(retrySuccessPath).forEach(path -> pollList.add(path));
            List<Path> tempList = tempMove.tempMove(pollList, context, txid);
            Files.delete(retrySuccessPath);
            isFindFile = true;
            finalTempList.addAll(tempList);
        }

        if (!isFindFile && Files.exists(retryTempPath)) {
            log.info("재전송할 파일을 찾음 ==> {}", retryTempPath);
            List<Path> tempList = new ArrayList<Path>();
            Files.list(retryTempPath).forEach(path -> tempList.add(path));
            isFindFile = true;
            finalTempList.addAll(tempList);
        }

        if (!isFindFile) {
            log.info("재전송할 파일을 찾지 못함 ==> {}", txid);
            return null;
        }


        FileMessage fileMessage = processFileToDB(context, txid, eventDt, finalTempList, true);

        return OnSignalInfo.builder().count(fileMessage.getCount()).processEnd(true).sendObject(fileMessage).build();
    }

    @Override
    public Object onMessageData(IFContext context, String txid, String eventDt, Object obj, Map<String, String> propMap) throws Exception {
        return null;
    }

    @Override
    public void onMessageResult(IFContext context, String txid, String eventDt, String resultCode, String resultMessage, String dataStr, Map<String, String> propMap) throws Exception {
    }


    private static void generateFileMessage(IFContext context, String txid, List<Path> fileNameList, FileMessage fileMessage) {
        List<FileInfo> fileInfoList = Lists.newArrayList();
        for (Path filePath : fileNameList) {
            long size = 0;
            try {
                size = Files.size(filePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileName(filePath.getFileName().toString());
            fileInfo.setSize(size);
            fileInfoList.add(fileInfo);
        }

        fileMessage.setFileInfoList(fileInfoList);
        fileMessage.setCount(fileInfoList.size());
    }
}
