package mb.fw.atb.strategy.pubsub;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.config.IFConfig;
import mb.fw.atb.config.Specifications;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.enums.TResult;
import mb.fw.atb.model.OnSignalInfo;
import mb.fw.atb.strategy.ATBStrategy;

import java.util.Map;

@Slf4j
public class ExampleStrategy extends ATBStrategy {

    @Override
    public Specifications specifications() {
        return null;
    }

    @Override
    public OnSignalInfo onSignal(IFContext context, String txid, String eventDt) throws Exception {

        String transactionId = txid;
        String initTime = eventDt;
        String interfaceId = context.getInterfaceId();
        String sendSystemCode = context.getSendSystemCode();
        String receiveSystemCode = context.getReceiveSystemCode();

        String id = config.getId();
        String address = config.getAddress();

        String sendJson = "{\"test\":\"test\"}";

        OnSignalInfo onSignalInfo = OnSignalInfo.builder().count(1).sendObject(sendJson).build();
        return onSignalInfo;
    }

    @Override
    public OnSignalInfo onSignalRetry(IFContext context, String txid, String msgCreDt, Map<String, String> propMap) throws Exception {
        return null;
    }

    @Override
    public Object onMessageData(IFContext context, String txid, String eventDt, Object data, Map<String, String> propMap) throws Exception {

        String jsonStr = (String) data;
        log.info("onMessageData : {}", data);

        String returnJson = "{\"test2\":\"test2\"}";

        return returnJson;
    }

    @Override
    public void onMessageResult(IFContext context, String txid, String eventDt, String resultCode, String resultMessage, String dataStr, Map<String, String> propMap) throws Exception {


        log.info("onMessageResult : {}", dataStr);
        log.info("resultCode : {}", resultCode);
        log.info("resultMessage : {}", resultMessage);

        TResult result = TResult.valueOf(resultCode);
        switch (result){
            case SUCCESS:
                log.info("SUCCESS");
                break;
            case FAIL:
                log.info("FAILURE");
                break;
        }

    }
}
