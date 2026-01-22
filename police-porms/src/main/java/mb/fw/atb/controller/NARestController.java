package mb.fw.atb.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.mb.indigo2.springsupport.AdaptorConfigBean;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTraceAspect;
import mb.fw.atb.config.sub.IFResult;
import mb.fw.atb.enums.THeader;
import mb.fw.atb.config.IFConfig;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.enums.TResult;
import mb.fw.atb.strategy.ATBStrategy;
import mb.fw.atb.util.ATBUtil;
import mb.fw.atb.util.DateUtils;
import mb.fw.atb.util.MDCLogging;
import mb.fw.atb.util.TransactionIdGenerator;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Network Adapter Rest Controller
 */
@RestController
@Slf4j
public class NARestController {

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private IFConfig config;

    @Autowired
    AdaptorConfigBean adaptorConfigBean;

    @Autowired(required = false)
    JmsTemplate jmsTemplate;

    private ObjectMapper mapper = new ObjectMapper();

    /**
     * 1:N 은 아직 미구현
     *
     * @param interfaceId
     * @param data
     * @return
     * @throws Exception
     */
    @PostMapping(path = "/na-api/{interfaceId}", produces = {MediaType.APPLICATION_JSON_VALUE}, consumes = {MediaType.APPLICATION_JSON_VALUE})
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

            ATBStrategy strategy = appContext.getBean(strategyName, ATBStrategy.class);
            resultMap = (Map) strategy.onMessageData(context, txid, msgCreDt, data, null);

            Map<String, String> ifResultMap = (Map) resultMap.get(THeader.IF_RESULT.key());

            //ATBStrategy 로부터 응답받은시간을 수신시간으로 하자
            String msgRcvDt = DateUtils.today17();
            ifResultMap.put(THeader.RECEIVER_MSG_RECV_DT.key(), msgRcvDt);
            String resultCd = ifResultMap.get(THeader.RECEIVER_RESULT_CD.key());
            String resultMessage = ifResultMap.get(THeader.RECEIVER_RESULT_MSG.key());
            String receiverId = ifResultMap.get(THeader.RECEIVER_ID.key());

            IFResult ifResult = config.getIfResult();

            if (!ifResult.isInject()) {
                resultMap.remove(THeader.IF_RESULT.key());
            }

            if (!ifResult.isTcpPlainData()) {
                ifResultMap.remove(THeader.TCP_SEND_MSG.key());
                ifResultMap.remove(THeader.TCP_RECV_MSG.key());
            }

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
            log.error("Exception : {}", e);
            throw e;
        } finally {
            MDCLogging.release();
        }
        return resultMap;
    }

    @PostMapping(path = "/na-api/echo", produces = {MediaType.APPLICATION_JSON_VALUE}, consumes = {MediaType.APPLICATION_JSON_VALUE})
    private Mono<Map> echo(@Header("interfaceId") String interfaceId, @Header("transactionId") String transactionId, @RequestBody Map data) throws Exception {

        log.info("postDummyResponse Receive : {}", data);
        log.info("postDummyResponse echo Return : {}", data);
        return Mono.just(data);
    }
    @PostMapping(path = "/na-api/nodata", produces = {MediaType.APPLICATION_JSON_VALUE}, consumes = {MediaType.APPLICATION_JSON_VALUE})
    private Mono<Map> nodata(@Header("interfaceId") String interfaceId, @Header("transactionId") String transactionId, @RequestBody Map data) throws Exception {

        log.info("postDummyResponse Receive : {}", data);
        LinkedHashMap<@Nullable Object, @Nullable Object> retMap = Maps.newLinkedHashMap();
        retMap.put("data", "");
        log.info("postDummyResponse nodata Return : {}", retMap);
        return Mono.just(retMap);
    }

    @GetMapping(path = "/{interfaceId}", produces = {MediaType.APPLICATION_JSON_VALUE}, consumes = {MediaType.APPLICATION_JSON_VALUE})
    private Mono<Map> getRequest(@PathVariable("interfaceId") String interfaceId, @RequestBody Map apiData) {
        log.info("GET METHOD IS UNSUPPORTED");
        return Mono.just(Maps.newLinkedHashMap());
    }

    public void startLogging(IFContext context, String txid, String msgCreDt, String sendDt, long count) throws Exception {
        ATBUtil.startLogging(jmsTemplate, context.getInterfaceId(), txid, context.getReceiverIds(), count, context.getSendSystemCode(), context.getReceiveSystemCode(), msgCreDt, sendDt);
    }

    public void endLogging(IFContext context, String txid, long errCount, String receiverId, String resultCd, String resultMessage, String msgRcvDt) throws Exception {
        ATBUtil.endLogging(jmsTemplate, context.getInterfaceId(), txid, receiverId, errCount, resultCd, resultMessage, msgRcvDt);
    }
}