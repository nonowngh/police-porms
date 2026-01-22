package mb.fw.atb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTrace;
import mb.fw.atb.aop.TimeTraceAspect;
import mb.fw.atb.config.IFConfig;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.strategy.na.StandardNetworkAdaptorStrategy;
import mb.fw.atb.util.DateUtils;
import mb.fw.atb.util.ATBUtil;
import mb.fw.atb.util.MDCLogging;
import mb.fw.net.common.NetworkAdaptorAPI;
import mb.fw.net.common.message.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class NAOutboundSendAsyncService {

    @Autowired
    ApplicationContext appContext;
    @Autowired(required = false)
    JmsTemplate jmsTemplate;

    ObjectMapper objectMapper = new ObjectMapper();

    @Async("threadPoolTaskExecutor")
    @TimeTrace
    public void asyncProcess(String orginTxid, String txid, String msg, IFConfig ifConfig, CallRequest callRequest, IFContext context, String transformType, String interfaceId, Map convertMap, Map inDataJson, String responseJson, String msgCreDt) throws Exception {
        try {
            MDCLogging.create(callRequest.getLnk_transaction_id(), callRequest.getLnk_interface_id(), callRequest.getLnk_adaptor());
            log.info("NAOutboundSendAsyncService.asyncProcess " + txid);
            NetworkAdaptorAPI networkAdaptorAPI = null;

            String receiverId = null;

            StandardNetworkAdaptorStrategy standardNetworkAdaptorStrategy = appContext.getBean(StandardNetworkAdaptorStrategy.class);

            log.info("standardNetworkAdaptorStrategy : " + standardNetworkAdaptorStrategy.toString());
            log.info("standardNetworkAdaptorStrategy.getEmbeddedNetworkAdaptor : " + ifConfig.getEmbeddedNetworkAdaptor());
            if (ifConfig.getEmbeddedNetworkAdaptor() != null) {
                networkAdaptorAPI = standardNetworkAdaptorStrategy.embeddedNetworkAdaptorAPI;
                receiverId = context.getReceiveSystemCode();
            } else {
                String dataReceiverId = ifConfig.getDataReceiverId();
                if (StringUtils.isNotEmpty(dataReceiverId)) {
                    receiverId = (String) inDataJson.get(dataReceiverId);
                    networkAdaptorAPI = appContext.getBean(receiverId, NetworkAdaptorAPI.class);
                } else {
                    receiverId = context.getReceiverIds()[0];
                    networkAdaptorAPI = appContext.getBean(context.getReceiverIds()[0], NetworkAdaptorAPI.class);
                }
            }

            TCPRequest request = new TCPRequest();
            request.setLnk_interface_id(context.getInterfaceId());
            request.setLnk_pattern(transformType);
            request.setLnk_src_org(context.getSendSystemCode());
            request.setLnk_trg_org(receiverId);
            request.setLnk_transaction_id(txid);
            request.setData(callRequest.getData());

            TCPRequestAck sendAck = networkAdaptorAPI.send(request);
            String resultCd = null;
            String resultMessage;
            String msgRcvDt = DateUtils.today17();

            String timeTraceStr = "### TIME_TRACE\n" + TimeTraceAspect.generateTimeTraceAndRemove(txid);

            if (sendAck.getLnk_result_cd().equals("0000")) {
                resultMessage = "### REQUEST\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(callRequest) + "\n\n" + "### CALL REQUEST\n" + convertMap + "\n\n" + "### CALL RESPONSE \n" + responseJson + "\n\n" + "### RESPONSE\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request) + "\n\n" + timeTraceStr;
                endLogging(context, orginTxid, 0, context.getReceiverIds()[0], "S", resultMessage);
            } else {
                resultMessage = "### REQUEST\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(callRequest) + "\n\n" + "### RESPONSE\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request) + "\n\n" + sendAck.getLnk_result_msg() + "\n\n" + timeTraceStr;
                ATBUtil.endLogging(jmsTemplate, context.getInterfaceId(), orginTxid, context.getReceiverIds()[0], 1, "F", resultMessage, msgRcvDt);
            }
        } finally {
            MDCLogging.release();
        }

    }

    public void startLogging(IFContext context, String txid, String msgCreDt, String sendDt, long count) throws Exception {
        ATBUtil.startLogging(jmsTemplate, context.getInterfaceId(), txid, context.getReceiverIds(), count, context.getSendSystemCode(), context.getReceiveSystemCode(), msgCreDt, sendDt);
    }

    public void endLogging(IFContext context, String txid, long errCount, String receiverId, String resultCd, String resultMessage) throws Exception {
        String msgRcvDt = DateUtils.today17();
        ATBUtil.endLogging(jmsTemplate, context.getInterfaceId(), txid, receiverId, errCount, resultCd, resultMessage, msgRcvDt);
    }
}
