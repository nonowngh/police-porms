package mb.fw.atb.job.com;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.enums.THeader;
import mb.fw.atb.config.IFConfig;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class ToJMSData {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public void send(JmsTemplate jmsTemplate, String destinationName, Object data, Map<String, String> propMap) throws Exception {
        jmsTemplate.convertAndSend(destinationName, data, message -> {
            for (String key : propMap.keySet()) {
                message.setStringProperty(key, propMap.get(key));
            }
            return message;
        });
    }

    public void send(JmsTemplate jmsTemplate, String destinationName, Object data, Map<String, String> propMap, String correlationId) throws Exception {
        jmsTemplate.convertAndSend(destinationName, data, message -> {
            message.setJMSCorrelationID(correlationId);
            for (String key : propMap.keySet()) {
                message.setStringProperty(key, propMap.get(key));
            }
            return message;
        });
    }

    public void eachSend(JmsTemplate jmsTemplate, IFConfig config, IFContext context, String txid, String msgCreDt, long count, String adaptorName, Object data, String dataClass, String timeTrace) {
        //1:N JMS송신 & 프로퍼티 생성
        Map propMap = Maps.newLinkedHashMap();
        for (String receiver : context.getReceiverIds()) {
            propMap.put(THeader.INTERFACE_ID.key(), context.getInterfaceId());
            propMap.put(THeader.TRANSACTION_ID.key(), txid);
            propMap.put(THeader.SENDER_ID.key(), config.getId());
            propMap.put(THeader.SENDER_MSG_CREATE_DT.key(), msgCreDt);
            propMap.put(THeader.SENDER_DATA_CLASS.key(), dataClass);
            propMap.put(THeader.SENDER_STRATEGY.key(), context.getStrategy());
            propMap.put(THeader.SENDER_MSG_SEND_DT.key(), DateUtils.today17());
            propMap.put(THeader.SENDER_ADAPTOR_NAME.key(), adaptorName);
            propMap.put(THeader.SENDER_DATA_COUNT.key(), String.valueOf(count));
            propMap.put(THeader.RECEIVER_ID.key(), receiver);
            propMap.put(THeader.TIME_TRACE.key(), timeTrace);

            try {
                send(jmsTemplate, config.getPrefix() + ".DATA", data, propMap);
            } catch (Exception e) {
                log.error("ToJMSData.send", e);
                throw new RuntimeException(e);
            }
        }
    }

    public String oneSend(JmsTemplate jmsTemplate, IFConfig config, IFContext context, String txid, String msgCreDt, long count, String adaptorName, Object data, String dataClass, String receiverId, boolean resendYn, String timeTrace) {

        String correlationId = UUID.randomUUID().toString();

        //1:N JMS송신 & 프로퍼티 생성
        Map propMap = Maps.newLinkedHashMap();
        propMap.put(THeader.INTERFACE_ID.key(), context.getInterfaceId());
        propMap.put(THeader.TRANSACTION_ID.key(), txid);
        propMap.put(THeader.SENDER_ID.key(), config.getId());
        propMap.put(THeader.SENDER_MSG_CREATE_DT.key(), msgCreDt);
        propMap.put(THeader.SENDER_DATA_CLASS.key(), dataClass);
        propMap.put(THeader.SENDER_STRATEGY.key(), context.getStrategy());
        propMap.put(THeader.SENDER_MSG_SEND_DT.key(), DateUtils.today17());
        propMap.put(THeader.SENDER_ADAPTOR_NAME.key(), adaptorName);
        propMap.put(THeader.SENDER_DATA_COUNT.key(), String.valueOf(count));
        propMap.put(THeader.RECEIVER_ID.key(), receiverId);
        propMap.put(THeader.RESEND_YN.key(), resendYn ? "Y" : "N");
        propMap.put(THeader.TIME_TRACE.key(), timeTrace);

        try {
            send(jmsTemplate, config.getPrefix() + ".DATA", data, propMap, correlationId);
        } catch (Exception e) {
            log.error("ToJMSData.send", e);
            throw new RuntimeException(e);
        }
        return correlationId;
    }

    public static Object waitingResult(JmsTemplate jmsTemplate, IFConfig config, String correlationId) {
        log.info("Receive JMSCorrelationID='" + correlationId + "'");
        return jmsTemplate.receiveSelectedAndConvert(config.getPrefix() + ".RESULT", "JMSCorrelationID='" + correlationId + "'");
    }
}
