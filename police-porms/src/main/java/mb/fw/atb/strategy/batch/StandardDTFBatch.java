package mb.fw.atb.strategy.batch;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTrace;
import mb.fw.atb.aop.TimeTraceAspect;
import mb.fw.atb.config.Specifications;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.enums.FileType;
import mb.fw.atb.enums.THeader;
import mb.fw.atb.enums.TResult;
import mb.fw.atb.error.ATBException;
import mb.fw.atb.error.ErrorCode;
import mb.fw.atb.job.file.DirectoryPolling;
import mb.fw.atb.job.file.IFTPJob;
import mb.fw.atb.job.file.TempMove;
import mb.fw.atb.model.OnSignalInfo;
import mb.fw.atb.model.data.DBMessage;
import mb.fw.atb.model.data.DetailData;
import mb.fw.atb.model.file.FileInfo;
import mb.fw.atb.model.file.FileMessage;
import mb.fw.atb.schema.batch.StandardDTFBatchSchema;
import mb.fw.atb.service.DBTransactionService;
import mb.fw.atb.strategy.ATBStrategy;
import mb.fw.atb.util.ATBUtil;
import mb.fw.atb.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DB TO FILE BATCH 재처리 구현 및 검증 필요
 */
@Component(value = "StandardDTFBatch")
@Slf4j
public class StandardDTFBatch extends ATBStrategy {

    @Autowired(required = false)
    @Qualifier("DBTransactionService")
    DBTransactionService dbTransactionService;

    @Autowired
    DirectoryPolling directoryPolling;

    @Autowired
    IFTPJob iftpJob;

    @Autowired
    TempMove tempMove;

    XmlMapper xmlMapper = new XmlMapper();

    public StandardDTFBatch() {
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * @return Specifications
     */
    @Override
    public Specifications specifications() {
        return StandardDTFBatchSchema.specifications();
    }

    @Override
    @TimeTrace
    public OnSignalInfo onSignal(IFContext context, String txid, String msgCreDt) throws Exception {
        Map contextMap = context.createContextMap();
        contextMap.put(THeader.SENDER_MSG_CREATE_DT.key(), msgCreDt);
        contextMap.put(THeader.TRANSACTION_ID.key(), txid);

        FileMessage fileMessage = new FileMessage();
        fileMessage.setFileInfoList(Lists.newArrayList());

        Path recvPath = null;
        Path errorPath = null;
        Path tempPath = null;

        String fileRecvPath = context.getFileRecvPath();
        String fileErrorPath = context.getFileErrorPath();
        String fileTempPath = context.getFileTempPath();

        recvPath = Paths.get(fileRecvPath);
        errorPath = Paths.get(fileErrorPath);
        tempPath = Paths.get(fileTempPath);

        log.info("RECV_PATH ==> {}", recvPath);
        directoryPolling.checkAndCreateDirectories(errorPath);
        directoryPolling.checkAndCreateDirectories(tempPath);
        directoryPolling.checkAndCreateDirectories(recvPath);

        log.debug("IFContext parameter Map {}", contextMap);
        List<Map<String, Object>> selectList = null;

        try {
            selectList = dbTransactionService.select(context.getInterfaceId() + ".SELECT", context, contextMap);
        } catch (Exception e) {
            ErrorCode errorCode = ErrorCode.SELECT_FAILURE;
            ATBException atbException = new ATBException("DB SELECT FAILURE", errorCode, e.getCause());
            log.error((errorCode.getStatus() + "," + errorCode.getCode() + "," + errorCode.getMessage()), atbException.getCause());
            throw atbException;
        }

        if (selectList == null || selectList.size() == 0) {
            log.info("NO DATA FOUND SCHEDULE STOPPED ");
            return null;
        }

        log.info("SELECT SIZE {}", selectList.size());

        try {
            String sendDt = DateUtils.today17();
            ATBUtil.startLogging(jmsTemplate, context.getInterfaceId(), txid, new String[]{config.getId()}, selectList.size(), context.getSendSystemCode(), context.getReceiveSystemCode(), msgCreDt, sendDt);
        } catch (Exception e) {
            ErrorCode errorCode = ErrorCode.JMS_SEND;
            ATBException atbException = new ATBException("JMS SEND FAILURE", errorCode, e.getCause());
            log.error((errorCode.getStatus() + "," + errorCode.getCode() + "," + errorCode.getMessage()), atbException.getCause());
            throw atbException;
        }

        boolean resendYn = false;

        Map propMap = Maps.newLinkedHashMap();
        propMap.put(THeader.INTERFACE_ID.key(), context.getInterfaceId());
        propMap.put(THeader.TRANSACTION_ID.key(), txid);
        propMap.put(THeader.SENDER_ID.key(), config.getId());
        propMap.put(THeader.SENDER_MSG_CREATE_DT.key(), msgCreDt);
        propMap.put(THeader.SENDER_STRATEGY.key(), context.getActionType());
        propMap.put(THeader.SENDER_MSG_SEND_DT.key(), DateUtils.today17());
        propMap.put(THeader.SENDER_ADAPTOR_NAME.key(), adaptorConfigBean.getAdaptorName());
        propMap.put(THeader.SENDER_DATA_COUNT.key(), String.valueOf(selectList.size()));
        propMap.put(THeader.RECEIVER_ID.key(), adaptorConfigBean.getAdaptorName());
        propMap.put(THeader.RESEND_YN.key(), resendYn ? "Y" : "N");


        DBMessage dbMessage = new DBMessage();
        dbMessage.setDataList(selectList);
        dbMessage.setCount(selectList.size());

        //Master-Detail 구현
        if (context.isDetailData()) {
            detailExtract(dbMessage, context, selectList);
            dbMessage.setDetailData(true);
        }

        try {
            dbTransactionService.updateList(context.getInterfaceId() + ".UPDATE", context, selectList, contextMap);
        } catch (Exception e) {
            ErrorCode errorCode = ErrorCode.UPDATE_FAILURE;
            ATBException atbException = new ATBException("UPDATE FAILURE", errorCode, e.getCause());
            log.error((errorCode.getStatus() + "," + errorCode.getCode() + "," + errorCode.getMessage()), atbException.getCause());
            throw atbException;
        }

        try {

            switch (FileType.valueOf(context.getFileType())) {
                case DELIMITER: {
                    log.info("DELIMITER FILE GENERATE");
                    char qualifier = context.getFileDelimiterQualifier();
                    boolean fileFirstHeader = context.isFileFirstHeader();
                    //qualifier는 미구현

                    Path saveFile = Paths.get(context.getFileRecvPath() + File.separator + txid + ".data");

                    char delimiter = context.getFileDelimiter();

                    // dbMessage to csv file generate
                    boolean isFirst = true;
                    for (Map<String, Object> dataList : dbMessage.getDataList()) {
                        Set<String> keySet = dataList.keySet();
                        if (isFirst) {
                            StringBuilder sb = new StringBuilder();
                            for (String key : keySet) {
                                sb.append(key).append(delimiter);
                            }
                            sb.deleteCharAt(sb.length() - 1);
                            sb.append("\n");
                            log.info(sb.toString());
                            Files.write(saveFile, sb.toString().getBytes());
                            isFirst = false;
                        }
                        StringBuilder sb = new StringBuilder();
                        for (String key : keySet) {
                            Object value = dataList.get(key);
                            if (value == null) {
                                sb.append(delimiter);
                            } else {
                                sb.append(value).append(delimiter);

                            }
                        }

                        sb.deleteCharAt(sb.length() - 1);
                        sb.append("\n");
                        Files.write(saveFile, sb.toString().getBytes(), java.nio.file.StandardOpenOption.APPEND);
                        log.debug("write : " + sb.toString());
                    }

                    FileInfo fileInfo = new FileInfo();
                    fileInfo.setFileName(saveFile.getFileName().toString());
                    fileInfo.setSize(Files.size(saveFile));
                    fileMessage.getFileInfoList().add(fileInfo);
                    fileMessage.setCount(dbMessage.getCount());

                    List<Map<String, DetailData>> detailList = dbMessage.getDetailList();

                    if (detailList != null) {
                        for (Map<String, DetailData> detailDataMap : detailList) {
                            for (String detailkey : detailDataMap.keySet()) {
                                Path detailFile = Paths.get(context.getFileRecvPath() + File.separator + txid + "." + detailkey);
                                DetailData detailData = detailDataMap.get(detailkey);
                                List<Map<String, Object>> detailDataList = detailData.getDataList();
                                for (Map<String, Object> detailDataMapList : detailDataList) {
                                    Set<String> keySet = detailDataMapList.keySet();

                                    if (isFirst) {
                                        StringBuilder sb = new StringBuilder();
                                        for (String key : keySet) {
                                            sb.append(key).append(delimiter);
                                        }
                                        sb.deleteCharAt(sb.length() - 1);
                                        sb.append("\n");
                                        log.info(sb.toString());
                                        Files.write(detailFile, sb.toString().getBytes());
                                        isFirst = false;
                                    }

                                    StringBuilder sb = new StringBuilder();
                                    for (String key : keySet) {
                                        Object value = detailDataMapList.get(key);
                                        if (value == null) {
                                            sb.append(delimiter);
                                        } else {
                                            sb.append(value).append(delimiter);
                                        }
                                    }
                                    sb.deleteCharAt(sb.length() - 1);
                                    sb.append("\n");
                                    Files.write(detailFile, sb.toString().getBytes(), java.nio.file.StandardOpenOption.APPEND);
                                    log.debug("detail write : " + sb.toString());
                                }
                                FileInfo detailfileInfo = new FileInfo();
                                detailfileInfo.setFileName(detailFile.getFileName().toString());
                                detailfileInfo.setSize(Files.size(detailFile));
                                fileMessage.getFileInfoList().add(detailfileInfo);
                                fileMessage.setCount(detailDataList.size());
                            }
                        }
                    }

                    break;
                }
                case FIXEDLENGTH: {
                    log.warn("FIXED_LENGTH unimplemented");
                    break;
                }
                case XML: {
                    log.info("XML FILE GENERATE");
                    Path path = Paths.get(context.getFileRecvPath() + File.separator + txid + ".xml");
                    xmlMapper.writeValue(path.toFile(), dbMessage);
                    FileInfo fileInfo = new FileInfo();
                    fileInfo.setFileName(path.getFileName().toString());
                    fileInfo.setSize(Files.size(path));
                    fileMessage.getFileInfoList().add(fileInfo);
                    fileMessage.setCount(dbMessage.getCount());
                    break;
                }
                case JSON: {
                    log.info("JSON FILE GENERATE");
                    Path path = Paths.get(context.getFileRecvPath() + File.separator + txid + ".json");
                    objectMapper.writeValue(Paths.get(context.getFileRecvPath() + File.separator + txid + ".json").toFile(), dbMessage);
                    FileInfo fileInfo = new FileInfo();
                    fileInfo.setFileName(path.getFileName().toString());
                    fileInfo.setSize(Files.size(path));
                    fileMessage.getFileInfoList().add(fileInfo);
                    fileMessage.setCount(dbMessage.getCount());
                    break;
                }
            }

            String msgRcvDt = DateUtils.today17();
            propMap.put(THeader.RECEIVER_MSG_RECV_DT.key(), msgRcvDt);
            propMap.put(THeader.RECEIVER_ADAPTOR_NAME.key(), adaptorConfigBean.getAdaptorName());
            propMap.put(THeader.RECEIVER_RESULT_CD.key(), TResult.SUCCESS.value());
            propMap.put(THeader.RECEIVER_RESULT_MSG.key(), "OK");
        } catch (Exception e) {
            ErrorCode errorCode = ErrorCode.FILE_GENERATE;
            ATBException atbException = new ATBException("FILE GENERATE FAILURE", errorCode, e.getCause());
            log.error((errorCode.getStatus() + "," + errorCode.getCode() + "," + errorCode.getMessage()), atbException.getCause());
            String msgRcvDt = DateUtils.today17();
            propMap.put(THeader.RECEIVER_MSG_RECV_DT.key(), msgRcvDt);
            propMap.put(THeader.RECEIVER_ADAPTOR_NAME.key(), adaptorConfigBean.getAdaptorName());
            propMap.put(THeader.RECEIVER_RESULT_CD.key(), TResult.FAIL.value());
            propMap.put(THeader.RECEIVER_RESULT_MSG.key(), "FILE GENERATE FAILURE : " + e.getMessage());
        }

        try {
            dbTransactionService.update(context.getInterfaceId() + ".RESULT_UPDATE", context, propMap, Maps.newLinkedHashMap());
        } catch (Exception e) {
            ErrorCode errorCode = ErrorCode.UPDATE_FAILURE;
            ATBException atbException = new ATBException("UPDATE FAILURE", errorCode, e.getCause());
            log.error((errorCode.getStatus() + "," + errorCode.getCode() + "," + errorCode.getMessage()), atbException.getCause());
        }

        String msgRcvDt = DateUtils.today17();
        String timeTraceStr = "### TIME_TRACE\n" + TimeTraceAspect.generateTimeTraceAndRemove(txid);
        String sendMessageJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(fileMessage);
        String propertyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(propMap);

        ATBUtil.endLogging(jmsTemplate, context.getInterfaceId(), txid, config.getId(), 0, (String) propMap.get(THeader.RECEIVER_RESULT_CD.key()), "### RESULT\n" + sendMessageJson + "\n\n### PROPERTY\n" + propertyJson + "\n\n" + timeTraceStr, msgRcvDt);

        return OnSignalInfo.builder().count(fileMessage.getCount()).processEnd(true).sendObject(fileMessage).build();
    }


    /**
     * Detail Data Extract
     *
     * @param dbMessage
     * @param context
     * @param selectList
     * @throws Exception
     */
    @TimeTrace
    public void detailExtract(DBMessage dbMessage, IFContext context, List<Map<String, Object>> selectList) throws
            Exception {

        List<Map<String, DetailData>> childList = Lists.newArrayList();
        log.info("DETAIL DATA PROCESSING");
        String[] childNames = context.getDetailNames();
        int i = 1;
        for (Map<String, Object> masterData : selectList) {
            Map<String, DetailData> multipleDataMap = Maps.newHashMap();
            StringBuilder sb = new StringBuilder();
            sb.append(i + " DETAIL : ");
            for (String childName : childNames) {
                List<Map<String, Object>> childDataList = dbTransactionService.selectNtime(context.getInterfaceId() + ".SELECT_" + childName, context, masterData);
                DetailData detailData = new DetailData();
                detailData.setName(childName);
                detailData.setDataList(childDataList);
                detailData.setSize(childDataList.size());
                multipleDataMap.put(childName, detailData);
                sb.append(childName).append("(").append(childDataList.size()).append("),");
            }
            sb.deleteCharAt(sb.length() - 1);
            log.info(sb.toString());
            i++;
            childList.add(multipleDataMap);
        }
        dbMessage.setDetailList(childList);

    }


    @Override
    @TimeTrace
    public OnSignalInfo onSignalRetry(IFContext context, String txid, String
            msgCreDt, Map<String, String> propMap) throws Exception {
        Map contextMap = context.createContextMap();

        contextMap.put(THeader.SENDER_MSG_CREATE_DT.key(), msgCreDt);

        return null;
    }

    @Override
    public Object onMessageData(IFContext context, String txid, String eventDt, Object
            data, Map<String, String> propMap) throws Exception {
        return null;
    }

    @Override
    public void onMessageResult(IFContext context, String txid, String eventDt, String resultCode, String
            resultMessage, String dataStr, Map<String, String> propMap) throws Exception {

    }

}
