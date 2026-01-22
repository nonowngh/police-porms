package mb.fw.atb.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indigo.esb.log.jms.ImsMsgHubEndData;
import com.indigo.esb.log.jms.ImsMsgHubStartData;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.model.ui.ATBModel;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import javax.jms.*;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * LoggingUtil -> ATBUtil 변경
 */
@Slf4j
public class ATBUtil {

    /**
     * 시작 로그
     *
     * @param jmsTemplate
     * @throws Exception
     */
    public static void startLogging(JmsTemplate jmsTemplate, String ifId, String txid, String[] destIds, long count, String sndCd, String rcvCd, String msgCreDt, String sndDt) throws Exception {
        ImsMsgHubStartData data = new ImsMsgHubStartData();
        data.setDestIds(destIds);
        data.setTxid(txid);
        data.setCount(count);
        data.setIfid(ifId);
        data.setSndCd(sndCd);
        data.setRcvCd(rcvCd);
        data.setMsgCredt(msgCreDt);
        data.setSndDt(msgCreDt);

        jmsTemplate.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        jmsTemplate.convertAndSend("ESB.LOGGING", data, message -> {
            message.setStringProperty("logType", "DETAIL");
            return message;
        });
    }

    /**
     * 종료 로그
     *
     * @param jmsTemplate
     * @throws Exception
     */
    public static void endLogging(JmsTemplate jmsTemplate, String ifId, String txid, String destId, long errCount, String resultCd, String resultMsg, String rcvDt) throws Exception {
        ImsMsgHubEndData data = new ImsMsgHubEndData();
        data.setDestId(destId);
        data.setTxid(txid);
        data.setErrCount(errCount);
        data.setIfid(ifId);
        data.setResultCd(resultCd);
        data.setResultMsg(resultMsg);
        data.setRcvDt(rcvDt);

        jmsTemplate.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        jmsTemplate.convertAndSend("ESB.LOGGING", data, message -> {
            message.setStringProperty("logType", "RESULT");
            return message;
        });
    }

    /**
     * 종료 로그(UPDATE)
     *
     * @param jmsTemplate
     * @throws Exception
     */
    public static void reEndLogging(JmsTemplate jmsTemplate, String ifId, String txid, String destId, long errCount,
                                    String resultCd, String resultMsg, String rcvDt) throws Exception {
        ImsMsgHubEndData data = new ImsMsgHubEndData();
        data.setDestId(destId);
        data.setTxid(txid);
        data.setErrCount(errCount);
        data.setIfid(ifId);
        data.setResultCd(resultCd);
        data.setResultMsg(resultMsg);
        data.setRcvDt(rcvDt);

        jmsTemplate.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        jmsTemplate.convertAndSend("ESB.LOGGING", data, message -> {
            message.setStringProperty("logType", "RE_RESULT");
            return message;
        });
    }

    /**
     * IFConfig.IFContext 가져올 정보를 불러온다
     *
     * @param jmsTemplate
     * @throws Exception
     */
    public static ATBModel myInterfaceContext(JmsTemplate jmsTemplate, String adaptorId) throws Exception {
        ObjectMapper om = new ObjectMapper();
        ImsMsgHubEndData data = new ImsMsgHubEndData();
        final String correlationId = UUID.randomUUID().toString();
        jmsTemplate.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

        jmsTemplate.send("ATB.MANAGER", new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                Destination resDestination = session.createQueue("ATB.MANAGER.RES");
                ObjectMessage reqMessage = session.createObjectMessage();
                reqMessage.setStringProperty("service", "MY_INTERFACE_INFO");
                reqMessage.setStringProperty("adaptorId", adaptorId);
                reqMessage.setJMSCorrelationID(correlationId);
                reqMessage.setJMSReplyTo(resDestination);
                return reqMessage;
            }
        });

        log.info("myInterfaceContext.JMSCorrelationID='" + correlationId + "'");
        Map configMap = (Map) jmsTemplate.receiveSelectedAndConvert("ATB.MANAGER.RES", "JMSCorrelationID='" + correlationId + "'");
        ATBModel retATBModel = new ATBModel();
        log.info("configMap  : {}", configMap);
        List<String> ifcontextJsonList = (List<String>) configMap.get("ifContext");
        for (String adaptorContext : ifcontextJsonList) {
            IFContext context = null;
            try {
                context = om.readValue(adaptorContext, IFContext.class);
                retATBModel.getIfContext().add(context);
            } catch (Exception e) {
                log.error("IFContext Parse Error : {}", e);
                log.error("error adaptorContext ==> {}", adaptorContext);
            }
        }
        return retATBModel;


    }

    public static void main(String[] args) throws InterruptedException {

        String[] destIds = new String[]{"D001"};
        String txid = "IF_001_123456789_006";
        String ifId = "IF_001";
        long count = 10;
        String sndCd = "S01";
        String rcvCd = "R01";
        String msgCreDt = DateFormatUtils.format(new Date(), "yyyyMMddHHmmssSSS");
        Thread.sleep(500);
        String sndDt = DateFormatUtils.format(new Date(), "yyyyMMddHHmmssSSS");
        JmsTemplate jmsTemplate = null;

        try {
            /**
             * 시작로그
             */
            ATBUtil.startLogging(jmsTemplate, ifId, txid, destIds, count, sndCd, rcvCd, msgCreDt, sndDt);

            String destId = "D001";
            long errCount = 10;
            String resultCd = "F";
            String resultMsg = "에러발생";


            /**
             * 종료로그
             */
            Thread.sleep(500);
            String rcvDt = DateFormatUtils.format(new Date(), "yyyyMMddHHmmssSSS");
            ATBUtil.endLogging(jmsTemplate, ifId, txid, destId, errCount, resultCd, resultMsg, rcvDt);


            /**
             * 재처리중 로그
             */
            Thread.sleep(2000);
            resultCd = "R";
            resultMsg = "재처리중...";
            rcvDt = DateFormatUtils.format(new Date(), "yyyyMMddHHmmssSSS");
            ATBUtil.reEndLogging(jmsTemplate, ifId, txid, destId, errCount, resultCd, resultMsg, rcvDt);


            /**
             * 재처리 완료
             */
            Thread.sleep(2000);
            resultCd = "S";
            resultMsg = "재처리로 처리 성공";
            rcvDt = DateFormatUtils.format(new Date(), "yyyyMMddHHmmssSSS");
            ATBUtil.reEndLogging(jmsTemplate, ifId, txid, destId, errCount, resultCd, resultMsg, rcvDt);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
