package mb.fw.atb.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.indigo.esb.nifs.IFTPClient;
import com.mb.indigo2.springsupport.AdaptorConfigBean;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTraceAspect;
import mb.fw.atb.enums.ActionType;
import mb.fw.atb.error.ATBException;
import mb.fw.atb.job.com.ToJMSData;
import mb.fw.atb.enums.THeader;
import mb.fw.atb.enums.TResult;
import mb.fw.atb.config.IFConfig;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.strategy.ATBStrategy;
import mb.fw.atb.util.ATBUtil;
import mb.fw.atb.util.DateUtils;
import mb.fw.atb.util.MDCLogging;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Map;

@Component
@Slf4j
public class DataListener implements MessageListener {

    ObjectMapper mapper = new ObjectMapper();

    @Autowired
    IFConfig config;

    @Autowired(required = false)
    JmsTemplate jmsTemplate;

    @Autowired
    AdaptorConfigBean adaptorConfigBean;

    @Autowired(required = false)
    IFTPClient client;

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

            propMap.put(THeader.JMSCorrelationID.key(), msg.getJMSCorrelationID());

        } catch (JMSException e) {
            throw new RuntimeException(e);
        }

        String interfaceId = propMap.get(THeader.INTERFACE_ID.key());
        IFContext context = config.findContext(interfaceId, ActionType.RECEIVER);
        String msgRcvDt = DateUtils.today17();
        String txid = (String) propMap.get(THeader.TRANSACTION_ID.key());
        String sendMessageJson = "{}";

        try {
            MDCLogging.create(txid, interfaceId, adaptorConfigBean.getAdaptorName());

            Span currentSpan = Span.current();
            currentSpan.setAttribute("transactionId", txid);
            currentSpan.setAttribute("interfaceId", context.getInterfaceId());
            currentSpan.setAttribute("adaptorName", adaptorConfigBean.getAdaptorName());

            log.info("DATA LISTENER START");
            log.debug("{} ", context);
            log.info("Sender's Property : {}", propMap);
            log.info("Sender's Data : {}", json);

            propMap.put(THeader.RECEIVER_ADAPTOR_NAME.key(), adaptorConfigBean.getAdaptorName());
            //이때는 수신시간으로 가지고 있다가
            propMap.put(THeader.RECEIVER_MSG_RECV_DT.key(), msgRcvDt);

            Object sendMessage = null;
            long count = propMap.get(THeader.SENDER_DATA_COUNT.key()) == null ? 0 : Long.parseLong(propMap.get(THeader.SENDER_DATA_COUNT.key()));
            long errCount = 0;

            String exceptionAsString = "";

            try {
                ATBStrategy strategy = null;
                String strategyName = context.getStrategy();
                if (StringUtils.isEmpty(strategyName)) {
                    log.error("Strategy is not defined");
                    throw new RuntimeException("Strategy is not defined");
                } else {
                    strategy = appContext.getBean(strategyName, ATBStrategy.class);
                }
                sendMessage = strategy.onMessageData(context, txid, msgRcvDt, json, propMap);

                msgRcvDt = DateUtils.today17();
                //최총 처리시간으로 다시 변경
                propMap.put(THeader.RECEIVER_MSG_RECV_DT.key(), msgRcvDt);

                if (!propMap.containsKey(THeader.RECEIVER_RESULT_CD.key())) {
                    //이상이 없으면 정상 응답설정
                    propMap.put(THeader.RECEIVER_RESULT_CD.key(), TResult.SUCCESS.value());
                    propMap.put(THeader.RECEIVER_RESULT_MSG.key(), "COMPLETE");
                } else {
                    errCount = count;
                }

            } catch (Exception e) {
                log.error("onMessageData Error", e);
                //이상이 없으면 정상 응답설정

                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                exceptionAsString = "###ERROR\n" + sw.toString();

                errCount = count;
                propMap.put(THeader.RECEIVER_RESULT_CD.key(), TResult.FAIL.value());
                //propMap.put(THeader.RECEIVER_RESULT_MSG.key(), exceptionAsString);
                propMap.put(THeader.RECEIVER_RESULT_MSG.key(), e.getMessage());

                //에러 처리도 하고 회신할 데이터가 필요할때 대비를 위하여 코드를 추가함
                if (e instanceof ATBException) {
                    ATBException atbException = (ATBException) e;
                    sendMessageJson = atbException.getReturnJson();
                }else{
                    sendMessageJson = "{}";
                }
            }

            String senderTimeTrace = propMap.get(THeader.TIME_TRACE.key());

            try {
                if (sendMessage != null) {
                    sendMessageJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(sendMessage);
                }

                if (config.isResultLogging()) {
                    String resendYn = propMap.get(THeader.RESEND_YN.key());
                    String timeTraceStr = TimeTraceAspect.generateTimeTraceAndRemove(txid);
                    if (StringUtils.isNotEmpty(timeTraceStr)) {
                        timeTraceStr = "### TIME_TRACE\n"+senderTimeTrace + timeTraceStr;
                    }else{
                        timeTraceStr = "### TIME_TRACE\n"+timeTraceStr;
                    }
                    propMap.remove(THeader.TIME_TRACE.key());

                    String propertyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(propMap);
                    if (resendYn == null || resendYn.equals("N")) {
                        ATBUtil.endLogging(jmsTemplate, interfaceId, txid, config.getId(), errCount, propMap.get(THeader.RECEIVER_RESULT_CD.key()), "### RESULT\n" + sendMessageJson + "\n\n### PROPERTY\n" + propertyJson + "\n\n" + timeTraceStr + "\n\n" + exceptionAsString, msgRcvDt);
                    } else {
                        ATBUtil.reEndLogging(jmsTemplate, interfaceId, txid, config.getId(), errCount, propMap.get(THeader.RECEIVER_RESULT_CD.key()), "### RESULT\n" + sendMessageJson + "\n\n### PROPERTY\n" + propertyJson + "\n\n" + timeTraceStr + "\n\n" + exceptionAsString, msgRcvDt);
                    }
                }
                log.info("sendMessageJson : {}", sendMessageJson);
                toJMSData.send(jmsTemplate, config.getPrefix() + ".RESULT", sendMessageJson, propMap);
            } catch (Exception e) {
                log.error("ToJMSData.send", e);
                throw new RuntimeException(e);
            }
        } finally {
            log.info("DATA LISTENER END");
            MDCLogging.release();
        }
    }
}
