package mb.fw.atb.strategy.pubsub;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTrace;
import mb.fw.atb.config.Specifications;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.enums.THeader;
import mb.fw.atb.error.ATBException;
import mb.fw.atb.error.ErrorCode;
import mb.fw.atb.model.OnSignalInfo;
import mb.fw.atb.model.data.DBMessage;
import mb.fw.atb.model.data.DetailData;
import mb.fw.atb.schema.pubsub.StandardDTDStrategySchema;
import mb.fw.atb.service.DBTransactionService;
import mb.fw.atb.strategy.ATBStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component(value = "StandardDTDStrategy")
@Slf4j
public class StandardDTDStrategy extends ATBStrategy {

    @Autowired(required = false)
    @Qualifier("DBTransactionService")
    DBTransactionService dbTransactionService;


    @Override
    public Specifications specifications() {
        return StandardDTDStrategySchema.specifications();
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

        Map contextMap = context.createContextMap();
        contextMap.put(THeader.SENDER_MSG_CREATE_DT.key(), msgCreDt);
        contextMap.put(THeader.TRANSACTION_ID.key(), txid);

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

        return OnSignalInfo.builder().count(selectList.size()).sendObject(dbMessage).build();

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
    public void detailExtract(DBMessage dbMessage, IFContext context, List<Map<String, Object>> selectList) throws Exception {

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
    public OnSignalInfo onSignalRetry(IFContext context, String txid, String msgCreDt, Map<String, String> propMap) throws Exception {
        Map contextMap = context.createContextMap();
        contextMap.put(THeader.SENDER_MSG_CREATE_DT.key(), msgCreDt);
        contextMap.put(THeader.TRANSACTION_ID.key(), txid);

        log.debug("IFContext parameter Map {}", contextMap);

        List<Map<String, Object>> selectList = null;

        try {
            selectList = dbTransactionService.select(context.getInterfaceId() + ".SELECT_RESEND", context, contextMap);
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


        return OnSignalInfo.builder().count(selectList.size()).sendObject(dbMessage).build();
    }

    @Override
    @TimeTrace
    public Object onMessageData(IFContext context, String txid, String eventDt, Object obj, Map<String, String> propMap) throws Exception {

        DBMessage dbMessage = objectMapper.readValue((String) obj, DBMessage.class);

        if (dbMessage.getDataList().size() != dbMessage.getCount()) {
            log.info("Data Size Not Matched {} != {}", dbMessage.getDataList().size(), dbMessage.getCount());
            ErrorCode errorCode = ErrorCode.VERIFICATION;
            ATBException atbException = new ATBException("Data Size Not Matched", errorCode, null);
            log.error(errorCode.getStatus() + "," + errorCode.getCode() + "," + errorCode.getMessage() + ",{}", atbException.getMessage());
            throw atbException;
        }

        if (dbMessage.isDetailData()) {
            log.info("CHILD DATA PROCESSING");
            dbTransactionService.insertMDList(dbMessage, context, propMap);
        } else {
            try {
                dbTransactionService.insertList(context.getInterfaceId() + ".INSERT", context, dbMessage.getDataList(), propMap);
                log.info("INSERT COMPLETE {}", dbMessage.getCount());
            } catch (Exception e) {
                ErrorCode errorCode = ErrorCode.INSERT_FAILURE;
                ATBException atbException = new ATBException("INSERT FAILURE", errorCode, e.getCause());
                log.error((errorCode.getStatus() + "," + errorCode.getCode() + "," + errorCode.getMessage()), atbException.getCause());
                throw atbException;
            }
        }


        if (context.isProcedureCall()) {
            try {
                dbTransactionService.call(context.getInterfaceId() + ".CALL", context, propMap);
                log.info("CALL COMPLETE {}", dbMessage.getCount());
            } catch (Exception e) {
                ErrorCode errorCode = ErrorCode.PROCEDURE_FAILURE;
                ATBException atbException = new ATBException("CALL FAILURE", errorCode, e.getCause());
                log.error((errorCode.getStatus() + "," + errorCode.getCode() + "," + errorCode.getMessage()), atbException.getCause());
                throw atbException;
            }
        }
        dbMessage.setDataList(Lists.newArrayList());
        dbMessage.setDetailList(Lists.newArrayList());
        return dbMessage;
    }

    @Override
    public void onMessageResult(IFContext context, String txid, String eventDt, String resultCode, String resultMessage, String dataStr, Map<String, String> propMap) throws Exception {
        try {
            DBMessage dbMessage = objectMapper.readValue((String) dataStr, DBMessage.class);
            dbTransactionService.update(context.getInterfaceId() + ".RESULT_UPDATE", context, propMap, Maps.newLinkedHashMap());
        } catch (Exception e) {
            ErrorCode errorCode = ErrorCode.UPDATE_FAILURE;
            ATBException atbException = new ATBException("UPDATE FAILURE", errorCode, e.getCause());
            log.error((errorCode.getStatus() + "," + errorCode.getCode() + "," + errorCode.getMessage()), atbException.getCause());
        }
    }


}
