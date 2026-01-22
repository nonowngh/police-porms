package mb.fw.atb.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.mb.indigo2.springsupport.AdaptorConfigBean;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTraceAspect;
import mb.fw.atb.config.IFConfig;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.enums.THeader;
import mb.fw.atb.enums.TResult;
import mb.fw.atb.job.com.ToJMSData;
import mb.fw.atb.strategy.ATBStrategy;
import mb.fw.atb.util.ATBUtil;
import mb.fw.atb.util.DateUtils;
import mb.fw.atb.util.MDCLogging;
import mb.fw.atb.util.TransactionIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AdaptorTemplate Rest Controller
 */
@RestController
@Slf4j
public class ATBRestController {

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private IFConfig config;

    @Autowired
    AdaptorConfigBean adaptorConfigBean;

    @Autowired(required = false)
    JmsTemplate jmsTemplate;

    @Autowired
    ToJMSData toJMSData;

    private ObjectMapper mapper = new ObjectMapper();

    /**
     * 1:N 은 아직 미구현
     *
     * @param interfaceId
     * @param data
     * @return
     * @throws Exception
     */
    @PostMapping(path = "/atb-api/{interfaceId}", produces = {MediaType.APPLICATION_JSON_VALUE}, consumes = {MediaType.APPLICATION_JSON_VALUE})
    private Map postRequest(@PathVariable("interfaceId") String interfaceId, @RequestBody Map data) throws Exception {

        Map resultMap = null;

        try {

            String msgCreDt = DateUtils.today17();
            String txid = TransactionIdGenerator.generate(interfaceId, "T", msgCreDt);
            MDCLogging.create(txid, interfaceId, adaptorConfigBean.getAdaptorName());

            Span currentSpan = Span.current();
            currentSpan.setAttribute("transactionId", txid);
            currentSpan.setAttribute("interfaceId", interfaceId);
            currentSpan.setAttribute("adaptorName", adaptorConfigBean.getAdaptorName());

            //인터페이스id 정보가 있는지 판단해서 잘못된거면 에러
            IFContext context = config.findContext(interfaceId);

            if (context == null) {
                log.error("interfaceId is not exist");
                LinkedHashMap<String, Object> retMap = Maps.newLinkedHashMap();
                LinkedHashMap<String, Object> retIfResult = Maps.newLinkedHashMap();
                retIfResult.put(THeader.INTERFACE_ID.key(), interfaceId);
                retIfResult.put(THeader.RECEIVER_RESULT_MSG.key(), "interfaceId is not exist");
                retIfResult.put(THeader.RECEIVER_RESULT_CD.key(), TResult.FAIL.value());
                retIfResult.put(THeader.TRANSACTION_ID.key(), txid);
                retIfResult.put(THeader.SENDER_MSG_SEND_DT.key(), "");
                retIfResult.put(THeader.RECEIVER_MSG_RECV_DT.key(), "");
                retMap.put(THeader.IF_RESULT.key(), retIfResult);
                return retMap;
            }

            startLogging(context, txid, msgCreDt, msgCreDt, 1);

            String strategyName = context.getStrategy();

            String receiverId = context.getReceiverIds()[0];

            switch (config.getRouteType()) {
                case "NONE":
                    LinkedHashMap propMap = Maps.newLinkedHashMap();
                    propMap.put(THeader.INTERFACE_ID.key(), interfaceId);
                    propMap.put(THeader.TRANSACTION_ID.key(), txid);

                    ATBStrategy strategy = appContext.getBean(strategyName, ATBStrategy.class);
                    resultMap = (Map) strategy.onMessageData(context, txid, msgCreDt, data, propMap);
                    break;
                case "JMS":
                    String dataStr = mapper.writeValueAsString(data);
                    String correlationId = toJMSData.oneSend(jmsTemplate, config, context, txid, msgCreDt, 1, adaptorConfigBean.getAdaptorName(), dataStr, dataStr.getClass().getCanonicalName(), context.getReceiverIds()[0], false, "");
                    String ackStr = (String) toJMSData.waitingResult(jmsTemplate, config, correlationId);

                    //log.info("ackStr : {}", ackStr);
                    Map ackMap = mapper.readValue(ackStr, Map.class);
                    //log.info("ackMap : {}", ackMap);
                    if (ackMap == null) {
                        log.error("JMS Timeout Error", ackMap);
                        //타임아웃 오류 응답
                        LinkedHashMap<String, Object> timeoutErrorMap = Maps.newLinkedHashMap();
                        LinkedHashMap<String, String> retIfResult = Maps.newLinkedHashMap();
                        retIfResult.put(THeader.INTERFACE_ID.key(), interfaceId);
                        retIfResult.put(THeader.RECEIVER_RESULT_MSG.key(), "JMS Timeout Error");
                        retIfResult.put(THeader.RECEIVER_RESULT_CD.key(), TResult.FAIL.value());
                        retIfResult.put(THeader.TRANSACTION_ID.key(), txid);
                        retIfResult.put(THeader.SENDER_MSG_SEND_DT.key(), "");
                        retIfResult.put(THeader.RECEIVER_MSG_RECV_DT.key(), "");
                        timeoutErrorMap.put(THeader.IF_RESULT.key(), retIfResult);
                        ackMap = timeoutErrorMap;
                    }
                    resultMap = ackMap;
                    break;
                default:
                    break;
            }
            //로그 이력을 남기기 위해 IF_RESULT 정보를 Strategy 로부터 받아야해
            Map<String, String> ifResult = (Map) resultMap.get(THeader.IF_RESULT.key());
            String msgRcvDt = DateUtils.today17();
            ifResult.put(THeader.RECEIVER_MSG_RECV_DT.key(), msgRcvDt);
            String resultCd = ifResult.get(THeader.RECEIVER_RESULT_CD.key());
            String resultMessage = ifResult.get(THeader.RECEIVER_RESULT_MSG.key());
            receiverId = ifResult.get(THeader.RECEIVER_ID.key());

            int errCount = 0;
            if (resultCd.equals(TResult.FAIL.value())) {
                errCount = 1;
            }

            String timeTraceStr = "### TIME_TRACE\n" + TimeTraceAspect.generateTimeTraceAndRemove(txid);
            String loggingResultMessage = "### REQUEST\n" + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data) + "\n\n" + "### RESPONSE\n" + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultMap) + "\n\n" + timeTraceStr;

            try {
                endLogging(context, txid, errCount, receiverId, resultCd, loggingResultMessage, msgRcvDt);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } catch (Exception e) {
            log.error("Exception : ", e);
            throw e;
        } finally {
            MDCLogging.release();
        }
        return resultMap;
    }

    @GetMapping(path = "/atb-api/{interfaceId}", produces = {MediaType.APPLICATION_JSON_VALUE}, consumes = {MediaType.APPLICATION_JSON_VALUE})
    private Mono<Map> getRequest(@PathVariable("interfaceId") String interfaceId, @RequestBody Map apiData) {
        log.info("GET METHOD IS UNSUPPORTED");
        return Mono.just(Maps.newLinkedHashMap());
    }

    public void startLogging(IFContext context, String txid, String msgCreDt, String sendDt, long count) throws Exception {
        for (String receiverId : context.getReceiverIds()) {
             log.info("startLogging receiverId : {}", receiverId);
        }

        ATBUtil.startLogging(jmsTemplate, context.getInterfaceId(), txid, context.getReceiverIds(), count, context.getSendSystemCode(), context.getReceiveSystemCode(), msgCreDt, sendDt);
    }

    public void endLogging(IFContext context, String txid, long errCount, String receiverId, String resultCd, String resultMessage, String msgRcvDt) throws Exception {
        ATBUtil.endLogging(jmsTemplate, context.getInterfaceId(), txid, receiverId, errCount, resultCd, resultMessage, msgRcvDt);
    }
}