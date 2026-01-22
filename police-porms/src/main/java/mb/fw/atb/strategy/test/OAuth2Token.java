package mb.fw.atb.strategy.test;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTrace;
import mb.fw.atb.config.Specifications;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.model.OnSignalInfo;
import mb.fw.atb.strategy.ATBStrategy;
import mb.fw.atb.util.oauth2.OAuth2RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component(value = "OAuth2Token")
@Slf4j
public class OAuth2Token extends ATBStrategy {

    @Override
    public Specifications specifications() {
        return null;
    }

    @Autowired(required = false)
    OAuth2RestTemplate oauth2RestTemplate;


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

        try {
            OAuth2AccessToken accessToken = oauth2RestTemplate.getAccessToken();
            log.info("accessToken: {}", accessToken);
        } catch (OAuth2Exception e) {
            log.error("OAuth2Exception: {}", e);
        }catch (Exception e) {
            log.error("Exception: ", e);
        }


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
