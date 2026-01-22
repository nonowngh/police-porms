package mb.fw.atb.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mb.indigo2.springsupport.AdaptorConfigBean;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.config.IFConfig;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.constant.ATBPrefix;
import mb.fw.atb.model.ui.ATBModel;
import mb.fw.atb.util.ATBUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

@Configuration
@Slf4j
public class AutoIfConfig {

    @Autowired(required = false)
    IFConfig ifconfig;

    @Autowired
    private AdaptorConfigBean adaptorConfigBean;

    @Autowired
    JmsTemplate jmsTemplate;


    @Bean(name = "autoIfCfg")
    public boolean autoIfCfg() {
        try {
            if (ifconfig.isAutoConfig()) {
                log.info("AutoIfConfig Start");
                ATBModel atbModel = ATBUtil.myInterfaceContext(jmsTemplate, adaptorConfigBean.getAdaptorName());
                if (atbModel.getIfContext().size() > 0) {
                    if (ifconfig.getContext() == null) {
                        ifconfig.setContext(atbModel.getIfContext());
                    } else {
                        ifconfig.getContext().addAll(atbModel.getIfContext());
                    }
                }
                log.info("AutoIfConfig End ==> {}", ifconfig.getContext());
            }
        } catch (Exception e) {
            log.error("AutoIfConfig Error", e);
            return false;
        }
        return true;
    }

    public static void main(String[] args) throws JsonProcessingException {
        String json = "{\n" +
                "  \"interfaceId\": \"IF_REST_DB_01\",\n" +
                "  \"actionType\": \"CUSTOMIZE\",\n" +
                "  \"strategy\": \"StandardDBInsertStrategy\",\n" +
                "  \"sendSystemCode\": \"A01\",\n" +
                "  \"receiveSystemCode\": \"B01\",\n" +
                "  \"receiverIds\": [\"REST_PROVIDER_DB_01\"]\n" +
                "}";

        ObjectMapper objectMapper = new ObjectMapper();
        IFContext dto = objectMapper.readValue(json, IFContext.class);
        System.out.println("Receiver IDs: " + dto.toString());
    }

}
