package mb.fw.atb.strategy.provider;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTrace;
import mb.fw.atb.config.Specifications;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.enums.THeader;
import mb.fw.atb.enums.TResult;
import mb.fw.atb.error.ATBException;
import mb.fw.atb.error.ErrorCode;
import mb.fw.atb.model.OnSignalInfo;
import mb.fw.atb.schema.provider.StandardDBUpdateStrategySchema;
import mb.fw.atb.service.DBTransactionService;
import mb.fw.atb.strategy.ATBStrategy;
import mb.fw.atb.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component(value = "StandardDBUpdateStrategy")
@Slf4j
public class StandardDBUpdateStrategy extends ATBStrategy {

    @Autowired(required = false)
    @Qualifier("DBTransactionService")
    DBTransactionService dbTransactionService;

    @Override
    public Specifications specifications() {
        return StandardDBUpdateStrategySchema.specifications();
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
    public OnSignalInfo onSignal(IFContext context, String txid, String msgCreDt) throws Exception {
        return null;
    }

    @Override
    public OnSignalInfo onSignalRetry(IFContext context, String txid, String msgCreDt, Map<String, String> propMap) throws Exception {
        return null;
    }

    @Override
    @TimeTrace
    public Object onMessageData(IFContext context, String txid, String eventDt, Object data, Map<String, String> propMap) throws Exception {
        Map dataMap = null;
        if (data instanceof String) {
            dataMap = objectMapper.readValue((String) data, Map.class);
        } else {
            dataMap = (Map) data;
        }

        List<Map<String, Object>> selectList = null;

        LinkedHashMap retMap = Maps.newLinkedHashMap();

        List dataList = (List) dataMap.get("dataList");
        if (dataList == null || dataList.size() == 0) {
            log.info("dataList Field --> Data Not Found");
            String msgRcvDt = DateUtils.today17();
            propMap.put(THeader.RECEIVER_ID.key(), config.getId());
            propMap.put(THeader.RECEIVER_STRATEGY.key(), context.getStrategy());
            propMap.put(THeader.RECEIVER_MSG_RECV_DT.key(), msgRcvDt);
            propMap.put(THeader.RECEIVER_ADAPTOR_NAME.key(), adaptorConfigBean.getAdaptorName());
            propMap.put(THeader.RECEIVER_RESULT_CD.key(), TResult.FAIL.value());
            propMap.put(THeader.RECEIVER_RESULT_MSG.key(), "dataList Field --> Data Not Found");
            retMap.put(THeader.IF_RESULT.key(), propMap);
            return retMap;
        }

        try {
            dbTransactionService.updateList(context.getInterfaceId() + ".UPDATE", context, dataList, propMap);
        } catch (Exception e) {
            ErrorCode errorCode = ErrorCode.UPDATE_FAILURE;
            ATBException atbException = new ATBException("DB UPDATE FAILURE", errorCode, e.getCause());
            log.error((errorCode.getStatus() + "," + errorCode.getCode() + "," + errorCode.getMessage()), atbException.getCause());

            String msgRcvDt = DateUtils.today17();
            propMap.put(THeader.RECEIVER_ID.key(), config.getId());
            propMap.put(THeader.RECEIVER_STRATEGY.key(), context.getStrategy());
            propMap.put(THeader.RECEIVER_MSG_RECV_DT.key(), msgRcvDt);
            propMap.put(THeader.RECEIVER_ADAPTOR_NAME.key(), adaptorConfigBean.getAdaptorName());
            propMap.put(THeader.RECEIVER_RESULT_CD.key(), TResult.FAIL.value());
            propMap.put(THeader.RECEIVER_RESULT_MSG.key(), "DB UPDATE FAILURE");
            retMap.put(THeader.IF_RESULT.key(), propMap);
            return retMap;
        }

        String msgRcvDt = DateUtils.today17();
        propMap.put(THeader.RECEIVER_ID.key(), config.getId());
        propMap.put(THeader.RECEIVER_STRATEGY.key(), context.getStrategy());
        propMap.put(THeader.RECEIVER_MSG_RECV_DT.key(), msgRcvDt);
        propMap.put(THeader.RECEIVER_ADAPTOR_NAME.key(), adaptorConfigBean.getAdaptorName());
        propMap.put(THeader.RECEIVER_RESULT_CD.key(), TResult.SUCCESS.value());
        propMap.put(THeader.RECEIVER_RESULT_MSG.key(), "SUCCESS");

        retMap.put(THeader.IF_RESULT.key(), propMap);

        return retMap;
    }

    @Override
    public void onMessageResult(IFContext context, String txid, String eventDt, String resultCode, String resultMessage, String dataStr, Map<String, String> propMap) throws Exception {
        return;
    }


}
