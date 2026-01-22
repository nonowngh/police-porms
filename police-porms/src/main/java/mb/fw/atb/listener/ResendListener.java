package mb.fw.atb.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.mb.indigo2.springsupport.AdaptorConfigBean;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTraceAspect;
import mb.fw.atb.config.IFConfig;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.enums.ActionType;
import mb.fw.atb.enums.THeader;
import mb.fw.atb.error.ATBException;
import mb.fw.atb.error.ErrorCode;
import mb.fw.atb.job.com.ToJMSData;
import mb.fw.atb.model.OnSignalInfo;
import mb.fw.atb.strategy.ATBStrategy;
import mb.fw.atb.util.DateUtils;
import mb.fw.atb.util.ATBUtil;
import mb.fw.atb.util.MDCLogging;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;

@Component
@Slf4j
public class ResendListener implements MessageListener {

    ObjectMapper mapper = new ObjectMapper();

    @Autowired
    IFConfig config;

    @Autowired(required = false)
    JmsTemplate jmsTemplate;

    @Autowired
    AdaptorConfigBean adaptorConfigBean;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    ToJMSData toJMSData;

    @Override
    public void onMessage(Message message) {
        if (!(message instanceof TextMessage)) {
            log.info("Data is not TextMessage");
            return;
        }
        TextMessage msg = (TextMessage) message;

        Map<String, String> propMap = null;
        String json = null;

        try {

            json = msg.getText();
            propMap = Maps.newLinkedHashMap();

            Enumeration propertyNames = msg.getPropertyNames();
            while (propertyNames.hasMoreElements()) {
                String name = (String) propertyNames.nextElement();
                String value = msg.getStringProperty(name);
                propMap.put(name, value);
            }

        } catch (JMSException e) {
            throw new RuntimeException(e);
        }

        String msgCreDt = DateUtils.today17();
        String interfaceId = propMap.get("IF_ID");
        IFContext context = config.findContext(interfaceId, ActionType.SENDER);
        String txid = (String) propMap.get("RESEND_TX_ID");
        String receiverId = (String) propMap.get("RESEND_DEST_ID");
        propMap.put(THeader.RECEIVER_ID.key(), receiverId);

        try {
            MDCLogging.create(txid, interfaceId , adaptorConfigBean.getAdaptorName());

            Span currentSpan = Span.current();
            currentSpan.setAttribute("transactionId", txid);
            currentSpan.setAttribute("interfaceId", context.getInterfaceId());
            currentSpan.setAttribute("adaptorName", adaptorConfigBean.getAdaptorName());


            log.info("SCHEDULE STARTED ,{}", txid);
            log.info("JMSTEMPLATE: {}", jmsTemplate);
            log.debug("{}", context);
            long count = 0;
            OnSignalInfo signalInfo = null;

            try {

                ATBStrategy strategy = null;
                String strategyName = context.getStrategy();
                if (StringUtils.isEmpty(strategyName)) {
                    log.info("UNREGISTERED STRATEGY: {},{}", context.getInterfaceId(), strategyName);
                    return;
                } else {
                    strategy = appContext.getBean(strategyName, ATBStrategy.class);
                }
                signalInfo = strategy.onSignalRetry(context, txid, msgCreDt , propMap);
            } catch (Exception e) {
                ErrorCode errorCode = ErrorCode.SCHEDULE_BATCH;
                ATBException atbException = new ATBException("SCHEDULE BATCH FAILURE", errorCode, e.getCause());
                log.error(errorCode.getStatus() + "," + errorCode.getCode() + "," + errorCode.getMessage() + ",{}", atbException.getCause().getMessage());
                throw atbException;
            }

            if (signalInfo == null || signalInfo.getCount() <= 0) {
                return;
            } else {
                count = signalInfo.getCount();
            }
            log.info("###### onSignalInfo {} :", signalInfo);

            if (!signalInfo.isProcessEnd()) {
                log.info("####### DataSend ######");

                String sendData = null;
                Object sendObject = signalInfo.getSendObject();

                if (sendObject instanceof String) {
                    sendData = (String) sendObject;
                } else {
                    try {
                        sendData = mapper.writeValueAsString(sendObject);
                    } catch (JsonProcessingException e) {
                        ErrorCode errorCode = ErrorCode.JSON_PARSE;
                        ATBException atbException = new ATBException("JSON PARSE FAILURE", errorCode, e.getCause());
                        log.error(errorCode.getStatus() + "," + errorCode.getCode() + "," + errorCode.getMessage() + ",{}", atbException.getCause().getMessage());
                        throw atbException;
                    }
                }

                String timeTraceStr = TimeTraceAspect.generateTimeTraceAndRemove(txid);

                toJMSData.oneSend(jmsTemplate, config, context, txid, msgCreDt, count, adaptorConfigBean.getAdaptorName(), sendData, signalInfo.getSendObject().getClass().getCanonicalName() , receiverId , true , timeTraceStr);

                if (config.isSendLogging()) {
                    try {
                        String rcvDt = DateFormatUtils.format(new Date(), "yyyyMMddHHmmssSSS");
                        ATBUtil.reEndLogging(jmsTemplate, interfaceId, txid, receiverId, 0, "R", "RESENDING..", rcvDt);
                    } catch (Exception e) {
                        ErrorCode errorCode = ErrorCode.JMS_SEND;
                        ATBException atbException = new ATBException("JMS SEND FAILURE", errorCode, e.getCause());
                        log.error(errorCode.getStatus() + "," + errorCode.getCode() + "," + errorCode.getMessage() + ",{}", atbException.getCause().getMessage());
                        throw atbException;
                    }
                }

            }


        } finally {
            MDCLogging.release();
        }


    }


    public static void main(String[] args) throws ClassNotFoundException, JsonProcessingException {

        String json = "{\n" +
                "\"invstRecvNo\" : null,\n" +
                "  \"perRecvNo\" : null,\n" +
                "  \"invstDocCd\" : null,\n" +
                "  \"userNm\" : null,\n" +
                "  \"userRrno\" : null,\n" +
                "  \"trprNm\" : null,\n" +
                "  \"rrno\" : null,\n" +
                "  \"bthdStart\" : null,\n" +
                "  \"bthdEnd\" : null,\n" +
                "  \"inptData\" : \"ew0KICAiUkVTUE9OU0UiIDogew0KICAgICJIRUFERVIiIDogew0KICAgICAgIlRMR1JfQ0QiIDogIlpINDIiLA0KICAgICAgIkpPQl9DTCIgOiAiIiwNCiAgICAgICJSRVFfQ0wiIDogIjAiLA0KICAgICAgIk5XX0NMIiA6ICJHIiwNCiAgICAgICJUTUxfSUQiIDogIjEwLjE3LjIwLjczIiwNCiAgICAgICJVU0VSX05NIiA6ICLIq7Hmtb8wMSIsDQogICAgICAiVVNFUl9SUk5PIiA6ICI2NzEwMTAxMDAwMDAwIiwNCiAgICAgICJRUllfQ0wiIDogIjEiLA0KICAgICAgIlJFU1BfQ0QiIDogIjAwIiwNCiAgICAgICJSRVFfQ05UIiA6ICIxIiwNCiAgICAgICJIVFRQX1BSUFIiIDogIlkiDQogICAgfSwNCiAgICAiREFUQSIgOiB7DQogICAgICAiTE9PUCIgOiBbIHsNCiAgICAgICAgIlJFQ09SRCIgOiB7DQogICAgICAgICAgIktPUl9GVUxOTSIgOiAiyKux5rW/MTExIiwNCiAgICAgICAgICAiUlJOTyIgOiAiODIwMjE3MTAwMDAwMCIsDQogICAgICAgICAgIkJUSEQiIDogIjE5ODIwMjE3IiwNCiAgICAgICAgICAiWkhNQV9KSU1VTl9OTyIgOiAiMTE0NDAyMTMzOSIsDQogICAgICAgICAgIlpITUFfU0VYIiA6ICIxIiwNCiAgICAgICAgICAiTFNfU1EiIDogIjAiLA0KICAgICAgICAgICJaSE1BX1JFR0lfR0IiIDogIjAiLA0KICAgICAgICAgICJaSE1BX0NITkdfU0FZVV9DRCIgOiAiMDEiLA0KICAgICAgICAgICJaSE1BX1JFR0lfU1RBVF9DRCIgOiAiMTAiLA0KICAgICAgICAgICJaSE1BX0pVTkdfR0IiIDogIjAiLA0KICAgICAgICAgICJaSE1BX0hFQURfTk0iIDogIsirsea1vzExMSIsDQogICAgICAgICAgIlpITUFfSEVBRF9KVU1JTl9OTyIgOiAiODIwMjE3MTAwMDAwMCIsDQogICAgICAgICAgIlpITUFfSEJfTEVHQUxfQ0QiIDogIjQ1MTkwMTE2MDAiLA0KICAgICAgICAgICJPVVRfQkpTSURPIiA6ICLA/LrPIiwNCiAgICAgICAgICAiT1VUX0JKU0lHVSIgOiAis7K/+L3DIiwNCiAgICAgICAgICAiT1VUX0JKRE9ORyIgOiAitbXF67W/IiwNCiAgICAgICAgICAiT1VUX0JKUkkiIDogIiIsDQogICAgICAgICAgIlpITUFfSEJfTVRfWU0iIDogIiIsDQogICAgICAgICAgIlpITUFfSEJfQlVOSkkiIDogIjIyMSIsDQogICAgICAgICAgIlpITUFfSEJfSE9TVSIgOiAiMCIsDQogICAgICAgICAgIlpITUFfSEJfUkRfQ0QxIiA6ICIiLA0KICAgICAgICAgICJaSE1BX0hCX1JEX0NEMyIgOiAiIiwNCiAgICAgICAgICAiT1VUX0JKX1JPQURfR1VCVU4iIDogIjIiLA0KICAgICAgICAgICJPVVRfQkpfUk9BRF9OTSIgOiAiIiwNCiAgICAgICAgICAiWkhNQV9IQl9SRF9KSUhBIiA6ICIiLA0KICAgICAgICAgICJaSE1BX0hCX1JEX0JPTk5PIiA6ICIiLA0KICAgICAgICAgICJaSE1BX0hCX1JEX0JVTk8iIDogIiIsDQogICAgICAgICAgIlpITUFfSEpfTEVHQUxfQ0QiIDogIjQ1MTEzMTA0MDAiLA0KICAgICAgICAgICJPVVRfSlNTSURPIiA6ICLA/LrPIiwNCiAgICAgICAgICAiT1VUX0pTU0lHVSIgOiAiwPzB1r3DtPbB+LG4IiwNCiAgICAgICAgICAiT1VUX0pTRE9ORyIgOiAiwM7IxLW/MrChIiwNCiAgICAgICAgICAiT1VUX0pTUkkiIDogIiIsDQogICAgICAgICAgIlpITUFfSEpfTVRfWU0iIDogIiIsDQogICAgICAgICAgIlpITUFfSEpfQlVOSkkiIDogIjAiLA0KICAgICAgICAgICJaSE1BX0hKX0hPU1UiIDogIjAiLA0KICAgICAgICAgICJaSE1BX0hKX1RPTkciIDogIjAiLA0KICAgICAgICAgICJaSE1BX0hKX0JBTiIgOiAiMCIsDQogICAgICAgICAgIk9VVF9US0pVU08iIDogIrDHwfa76sDMwfa/8r7GxsTGriIsDQogICAgICAgICAgIlpITUFfSEpfVFVLX0RPTkciIDogIjEwMiIsDQogICAgICAgICAgIlpITUFfSEpfUkRfQ0hFVU5HIiA6ICIiLA0KICAgICAgICAgICJaSE1BX0hKX1RVS19IT1NVIiA6ICIxMjA0IiwNCiAgICAgICAgICAiWkhNQV9ISl9SRF9DRDEiIDogIjQ1MTEzIiwNCiAgICAgICAgICAiWkhNQV9ISl9SRF9DRDMiIDogIjQ2MDExODciLA0KICAgICAgICAgICJPVVRfSEpfUk9BRF9HVUJVTiIgOiAiMSIsDQogICAgICAgICAgIk9VVF9ISl9ST0FEX05NIiA6ICK56bW/MrHmIiwNCiAgICAgICAgICAiWkhNQV9ISl9SRF9KSUhBIiA6ICIiLA0KICAgICAgICAgICJaSE1BX0hKX1JEX0JPTk5PIiA6ICIxMSIsDQogICAgICAgICAgIlpITUFfSEpfUkRfQlVOTyIgOiAiIiwNCiAgICAgICAgICAiWkhNQV9ISl9KVU5JUF9EVCIgOiAiMjAxNjA5MTIiLA0KICAgICAgICAgICJaSE1BX1JFR0lfQ0hOR19EVCIgOiAiMjAxNjA5MTIiLA0KICAgICAgICAgICJaSE1BX0hJU1RfTk8iIDogIjAiLA0KICAgICAgICAgICJPVVRfU1VCQUVfWU4iIDogIiIsDQogICAgICAgICAgIk9VVF9KQ01NX09XTkVSX1lOIiA6ICIiLA0KICAgICAgICAgICJPVVRfSkNDTV9PV05FUl9ZTiIgOiAiIiwNCiAgICAgICAgICAiT1VUX0pDSE1fT1dORVJfWU4iIDogIiINCiAgICAgICAgfQ0KICAgICAgfSwgew0KICAgICAgICAiUkVDT1JEIiA6IHsNCiAgICAgICAgICAiS09SX0ZVTE5NIiA6ICLIq7Hmtb8yMjIyIiwNCiAgICAgICAgICAiUlJOTyIgOiAiODIwMjE3MTAwMDAwMCIsDQogICAgICAgICAgIkJUSEQiIDogIjE5ODIwMjE3IiwNCiAgICAgICAgICAiWkhNQV9KSU1VTl9OTyIgOiAiMTE0NDAyMTMzOSIsDQogICAgICAgICAgIlpITUFfU0VYIiA6ICIxIiwNCiAgICAgICAgICAiTFNfU1EiIDogIjAiLA0KICAgICAgICAgICJaSE1BX1JFR0lfR0IiIDogIjAiLA0KICAgICAgICAgICJaSE1BX0NITkdfU0FZVV9DRCIgOiAiMDEiLA0KICAgICAgICAgICJaSE1BX1JFR0lfU1RBVF9DRCIgOiAiMTAiLA0KICAgICAgICAgICJaSE1BX0pVTkdfR0IiIDogIjAiLA0KICAgICAgICAgICJaSE1BX0hFQURfTk0iIDogIsirsea1vzIyMjIiLA0KICAgICAgICAgICJaSE1BX0hFQURfSlVNSU5fTk8iIDogIjgyMDIxNzEwMDAwMDAiLA0KICAgICAgICAgICJaSE1BX0hCX0xFR0FMX0NEIiA6ICI0NTE5MDExNjAwIiwNCiAgICAgICAgICAiT1VUX0JKU0lETyIgOiAiwPy6zyIsDQogICAgICAgICAgIk9VVF9CSlNJR1UiIDogIrOyv/i9wyIsDQogICAgICAgICAgIk9VVF9CSkRPTkciIDogIrW1xeu1vyIsDQogICAgICAgICAgIk9VVF9CSlJJIiA6ICIiLA0KICAgICAgICAgICJaSE1BX0hCX01UX1lNIiA6ICIiLA0KICAgICAgICAgICJaSE1BX0hCX0JVTkpJIiA6ICIyMjEiLA0KICAgICAgICAgICJaSE1BX0hCX0hPU1UiIDogIjAiLA0KICAgICAgICAgICJaSE1BX0hCX1JEX0NEMSIgOiAiIiwNCiAgICAgICAgICAiWkhNQV9IQl9SRF9DRDMiIDogIiIsDQogICAgICAgICAgIk9VVF9CSl9ST0FEX0dVQlVOIiA6ICIyIiwNCiAgICAgICAgICAiT1VUX0JKX1JPQURfTk0iIDogIiIsDQogICAgICAgICAgIlpITUFfSEJfUkRfSklIQSIgOiAiIiwNCiAgICAgICAgICAiWkhNQV9IQl9SRF9CT05OTyIgOiAiIiwNCiAgICAgICAgICAiWkhNQV9IQl9SRF9CVU5PIiA6ICIiLA0KICAgICAgICAgICJaSE1BX0hKX0xFR0FMX0NEIiA6ICI0NTExMzEwNDAwIiwNCiAgICAgICAgICAiT1VUX0pTU0lETyIgOiAiwPy6zyIsDQogICAgICAgICAgIk9VVF9KU1NJR1UiIDogIsD8wda9w7T2wfixuCIsDQogICAgICAgICAgIk9VVF9KU0RPTkciIDogIsDOyMS1vzKwoSIsDQogICAgICAgICAgIk9VVF9KU1JJIiA6ICIiLA0KICAgICAgICAgICJaSE1BX0hKX01UX1lNIiA6ICIiLA0KICAgICAgICAgICJaSE1BX0hKX0JVTkpJIiA6ICIwIiwNCiAgICAgICAgICAiWkhNQV9ISl9IT1NVIiA6ICIwIiwNCiAgICAgICAgICAiWkhNQV9ISl9UT05HIiA6ICIwIiwNCiAgICAgICAgICAiWkhNQV9ISl9CQU4iIDogIjAiLA0KICAgICAgICAgICJPVVRfVEtKVVNPIiA6ICKwx8H2u+rAzMH2v/K+xsbExq4iLA0KICAgICAgICAgICJaSE1BX0hKX1RVS19ET05HIiA6ICIxMDIiLA0KICAgICAgICAgICJaSE1BX0hKX1JEX0NIRVVORyIgOiAiIiwNCiAgICAgICAgICAiWkhNQV9ISl9UVUtfSE9TVSIgOiAiMTIwNCIsDQogICAgICAgICAgIlpITUFfSEpfUkRfQ0QxIiA6ICI0NTExMyIsDQogICAgICAgICAgIlpITUFfSEpfUkRfQ0QzIiA6ICI0NjAxMTg3IiwNCiAgICAgICAgICAiT1VUX0hKX1JPQURfR1VCVU4iIDogIjEiLA0KICAgICAgICAgICJPVVRfSEpfUk9BRF9OTSIgOiAiuem1vzKx5iIsDQogICAgICAgICAgIlpITUFfSEpfUkRfSklIQSIgOiAiIiwNCiAgICAgICAgICAiWkhNQV9ISl9SRF9CT05OTyIgOiAiMTEiLA0KICAgICAgICAgICJaSE1BX0hKX1JEX0JVTk8iIDogIiIsDQogICAgICAgICAgIlpITUFfSEpfSlVOSVBfRFQiIDogIjIwMTYwOTEyIiwNCiAgICAgICAgICAiWkhNQV9SRUdJX0NITkdfRFQiIDogIjIwMTYwOTEyIiwNCiAgICAgICAgICAiWkhNQV9ISVNUX05PIiA6ICIwIiwNCiAgICAgICAgICAiT1VUX1NVQkFFX1lOIiA6ICIiLA0KICAgICAgICAgICJPVVRfSkNNTV9PV05FUl9ZTiIgOiAiIiwNCiAgICAgICAgICAiT1VUX0pDQ01fT1dORVJfWU4iIDogIiIsDQogICAgICAgICAgIk9VVF9KQ0hNX09XTkVSX1lOIiA6ICIiDQogICAgICAgIH0NCiAgICAgIH0gXQ0KICAgIH0NCiAgfQ0KfQ==\"\n" +
                "}";

        ObjectMapper om = new ObjectMapper();
        // Object swmessage = om.readValue(json, Class.forName("mb.fw.atb.model.data.SinwonMessage").getClass());
        Object swmessage = om.readValue(json, Class.forName("mb.fw.atb.model.data.SinwonMessage"));
        System.out.println("print : " + swmessage);

    }
}
