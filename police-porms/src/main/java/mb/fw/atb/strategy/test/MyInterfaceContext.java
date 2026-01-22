package mb.fw.atb.strategy.test;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTrace;
import mb.fw.atb.config.Specifications;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.model.OnSignalInfo;
import mb.fw.atb.model.ui.ATBModel;
import mb.fw.atb.strategy.ATBStrategy;
import mb.fw.atb.util.ATBUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component(value = "MyInterfaceContext")
@Slf4j
public class MyInterfaceContext extends ATBStrategy {

    @Override
    public Specifications specifications() {
        return null;
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
        ATBModel atbModel = null;
        try {
            atbModel = ATBUtil.myInterfaceContext(jmsTemplate, adaptorConfigBean.getAdaptorName());
        } catch (Exception e) {
            log.error("IFContext Error : {}", e.getMessage());
        }
        log.info("IFContext : {}", atbModel);

        return OnSignalInfo.builder().count(1).processEnd(true).build();
    }

    @Override
    public OnSignalInfo onSignalRetry(IFContext context, String txid, String eventDt, Map<String, String> propMap) throws Exception {
        return null;
    }

    @Override
    public Object onMessageData(IFContext context, String txid, String eventDt, Object obj
            , Map<String, String> propMap) throws Exception {
        return null;
    }

    @Override
    public void onMessageResult(IFContext context, String txid, String eventDt, String resultCode, String
            resultMessage, String jsonStr, Map<String, String> propMap) throws Exception {
    }
}
