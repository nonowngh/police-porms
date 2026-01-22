package mb.fw.atb.strategy.na;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.mb.indigo2.springsupport.AdaptorConfigBean;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTrace;
import mb.fw.atb.config.Specifications;
import mb.fw.atb.config.sub.EmbeddedNetworkAdaptor;
import mb.fw.atb.enums.THeader;
import mb.fw.atb.config.IFConfig;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.enums.TResult;
import mb.fw.atb.model.OnSignalInfo;
import mb.fw.atb.parser.ATBParser;
import mb.fw.atb.schema.na.StandardNetworkAdaptorStrategySchema;
import mb.fw.atb.service.NAInboundAsyncService;
import mb.fw.atb.strategy.ATBStrategy;
import mb.fw.atb.util.DateUtils;
import mb.fw.net.common.NetworkAdaptor;
import mb.fw.net.common.NetworkAdaptorAPI;
import mb.fw.net.common.message.TCPRequest;
import mb.fw.net.common.message.TCPRequestAck;
import mb.fw.net.common.message.TCPResponse;
import mb.fw.net.fixedlength.FixedLengthExtractor;
import mb.fw.net.fixedlength.FixedLengthNetworkAdaptor;
import mb.fw.net.fixedlength.controller.FixedLengthRestController;
import mb.fw.net.tcpdelimiter.TcpdelimiterExtractor;
import mb.fw.net.tcpdelimiter.TcpdelimiterNetworkAdaptor;
import mb.fw.net.tcpdelimiter.controller.TcpdelimiterRestController;
import mb.fw.net.tcplength.TcplengthExtractor;
import mb.fw.net.tcplength.TcplengthNetworkAdaptor;
import mb.fw.net.tcplength.controller.TcpLengthRestController;
import mb.fw.transformation.engine.AsciiEngine;
import mb.fw.transformation.engine.MapEngine;
import mb.fw.transformation.form.MessageForm;
import mb.fw.transformation.form.MessageFormBox;
import mb.fw.transformation.tool.TypeConversion;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;

@Component(value = "StandardNetworkAdaptorStrategy")
@Slf4j
public class StandardNetworkAdaptorStrategy extends ATBStrategy {

    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    IFConfig ifConfig;

    @Autowired
    protected ApplicationContext appContext;

    MapEngine mapEngine = new MapEngine();
    AsciiEngine asciiEngine = new AsciiEngine();

    @Autowired(required = false)
    @Qualifier("myMessageFormBox")
    public HashMap<String, MessageFormBox> myMessageFormBox;

    @Autowired
    AdaptorConfigBean bean;

    @Autowired(required = false)
    NAInboundAsyncService naInboundAsyncService;

    @Autowired(required = false)
    JmsTemplate jmsTemplate;

    public static NetworkAdaptorAPI embeddedNetworkAdaptorAPI;

    @Autowired
    Environment env;

    public static EmbeddedNetworkAdaptor embeddedNA;

    NetworkAdaptor adaptor = null;

    Base64.Encoder base64Encoder = Base64.getEncoder();
    Base64.Decoder base64Decoder = Base64.getDecoder();

    @Autowired
    ApplicationContext applicationContext;

    @PreDestroy
    public void destroy() {
        if (adaptor != null) {
            adaptor.stop();
        }
    }

    @PostConstruct
    @TimeTrace
    public void init() throws Exception {

        log.info("StandardNetworkAdaptorStrategy ==>" + this.toString());
        int[] bindPorts = new int[1];

        embeddedNA = ifConfig.getEmbeddedNetworkAdaptor();
        log.info("EmbeddedNetworkAdaptor ==> " + embeddedNA);

        if (embeddedNA != null) {
            log.info("EmbeddedNetworkAdaptorAPIStrategy init() start");
            switch (embeddedNA.getType()) {

                case "LENGTH": {
                    TcplengthExtractor extractor = new TcplengthExtractor();
                    extractor.setMessageId(embeddedNA.getMessageId());
                    extractor.setMessageIdLen(embeddedNA.getMessageIdLen());
                    extractor.setWorkCode(embeddedNA.getWorkCode());
                    extractor.setWorkCodeLen(embeddedNA.getWorkCodeLen());

                    TcplengthNetworkAdaptor networkAdaptor = new TcplengthNetworkAdaptor();
                    this.adaptor = networkAdaptor;
                    TcpLengthRestController tcpLengthRestController = new TcpLengthRestController();
                    tcpLengthRestController.setAdaptor(networkAdaptor);
                    tcpLengthRestController.setBean(bean);
                    tcpLengthRestController.setExtractor(extractor);
                    embeddedNetworkAdaptorAPI = tcpLengthRestController;

                    networkAdaptor.setGroupId(embeddedNA.getGroupId());
                    networkAdaptor.setBindPorts(embeddedNA.getBindPorts());
                    networkAdaptor.setCallType(embeddedNA.getCallType());
                    networkAdaptor.setConnectTimeoutSec(embeddedNA.getConnectTimeoutSec());
                    networkAdaptor.setRemoteHosts(embeddedNA.getRemoteHosts());
                    networkAdaptor.setRemotePorts(embeddedNA.getRemotePorts());
                    networkAdaptor.setMaxFrameLength(embeddedNA.getMaxFrameLength());
                    networkAdaptor.setLengthAdjustment(embeddedNA.getLengthAdjustment());
                    networkAdaptor.setLengthFieldLength(embeddedNA.getLengthFieldLength());
                    networkAdaptor.setLengthFieldOffset(embeddedNA.getLengthFieldOffset());
                    networkAdaptor.setTcpMode(embeddedNA.getTcpMode());
                    networkAdaptor.putManualSet(env, extractor, bean, naInboundAsyncService, jmsTemplate);
                    networkAdaptor.setSrcOrg(embeddedNA.getSrcOrg());
                    networkAdaptor.setTrgOrg(embeddedNA.getTrgOrg());
                    networkAdaptor.setLoggingType(embeddedNA.getLoggingType());
                    networkAdaptor.start();
                    break;
                }
                case "DELIMITER": {
                    TcpdelimiterExtractor extractor = new TcpdelimiterExtractor();
                    extractor.setMessageId(embeddedNA.getMessageId());
                    extractor.setMessageIdLen(embeddedNA.getMessageIdLen());
                    extractor.setWorkCode(embeddedNA.getWorkCode());
                    extractor.setWorkCodeLen(embeddedNA.getWorkCodeLen());

                    //나중에 분기하자
                    TcpdelimiterNetworkAdaptor networkAdaptor = new TcpdelimiterNetworkAdaptor();
                    this.adaptor = networkAdaptor;
                    TcpdelimiterRestController tcpdelimiterRestController = new TcpdelimiterRestController();
                    tcpdelimiterRestController.setAdaptor(networkAdaptor);
                    tcpdelimiterRestController.setBean(bean);
                    tcpdelimiterRestController.setExtractor(extractor);
                    embeddedNetworkAdaptorAPI = tcpdelimiterRestController;

                    networkAdaptor.setGroupId(embeddedNA.getGroupId());
                    networkAdaptor.setBindPorts(embeddedNA.getBindPorts());
                    networkAdaptor.setCallType(embeddedNA.getCallType());
                    networkAdaptor.setConnectTimeoutSec(embeddedNA.getConnectTimeoutSec());
                    networkAdaptor.setRemoteHosts(embeddedNA.getRemoteHosts());
                    networkAdaptor.setRemotePorts(embeddedNA.getRemotePorts());
                    networkAdaptor.setMaxFrameLength(embeddedNA.getMaxFrameLength());
                    networkAdaptor.setHexDelimiters(embeddedNA.getHexDelimiters());
                    networkAdaptor.setAddLastHexDelimiter(embeddedNA.getAddLastHexDelimiter());
                    networkAdaptor.setTcpMode(embeddedNA.getTcpMode());
                    networkAdaptor.putManualSet(env, extractor, bean, naInboundAsyncService, jmsTemplate);
                    networkAdaptor.setSrcOrg(embeddedNA.getSrcOrg());
                    networkAdaptor.setTrgOrg(embeddedNA.getTrgOrg());
                    networkAdaptor.setLoggingType(embeddedNA.getLoggingType());
                    networkAdaptor.start();
                    break;
                }


                case "FIXED": {
                    FixedLengthExtractor extractor = new FixedLengthExtractor();
                    extractor.setMessageId(embeddedNA.getMessageId());
                    extractor.setMessageIdLen(embeddedNA.getMessageIdLen());
                    extractor.setWorkCode(embeddedNA.getWorkCode());
                    extractor.setWorkCodeLen(embeddedNA.getWorkCodeLen());

                    //나중에 분기하자
                    FixedLengthNetworkAdaptor networkAdaptor = new FixedLengthNetworkAdaptor();
                    this.adaptor = networkAdaptor;
                    FixedLengthRestController fixedLengthRestController = new FixedLengthRestController();
                    fixedLengthRestController.setAdaptor(networkAdaptor);
                    fixedLengthRestController.setBean(bean);
                    fixedLengthRestController.setExtractor(extractor);
                    embeddedNetworkAdaptorAPI = fixedLengthRestController;

                    networkAdaptor.setGroupId(embeddedNA.getGroupId());
                    networkAdaptor.setBindPorts(embeddedNA.getBindPorts());
                    networkAdaptor.setCallType(embeddedNA.getCallType());
                    networkAdaptor.setConnectTimeoutSec(embeddedNA.getConnectTimeoutSec());
                    networkAdaptor.setRemoteHosts(embeddedNA.getRemoteHosts());
                    networkAdaptor.setRemotePorts(embeddedNA.getRemotePorts());
                    networkAdaptor.setFrameLength(embeddedNA.getFrameLength());
                    networkAdaptor.setTcpMode(embeddedNA.getTcpMode());
                    networkAdaptor.putManualSet(env, extractor, bean, naInboundAsyncService, jmsTemplate);
                    networkAdaptor.setSrcOrg(embeddedNA.getSrcOrg());
                    networkAdaptor.setTrgOrg(embeddedNA.getTrgOrg());
                    networkAdaptor.setLoggingType(embeddedNA.getLoggingType());
                    networkAdaptor.setSendClose(embeddedNA.isSendClose());
                    networkAdaptor.start();
                    break;
                }
            }
        }
    }

    @Override
    public Specifications specifications() {
        return StandardNetworkAdaptorStrategySchema.specifications();
    }

    @Override
    public OnSignalInfo onSignal(IFContext context, String txid, String msgCreDt) throws Exception {
        return null;
    }

    @Override
    public OnSignalInfo onSignalRetry(IFContext context, String txid, String eventDt, Map<String, String> propMap) throws Exception {
        return null;
    }

    /**
     * json example
     * {
     * "name": "test",
     * "age": 20
     * }
     *
     * @param context     context
     * @param txid        TransactionId
     * @param msgCreDt    이벤트 발생시간
     * @param requestData 요청 데이터
     * @param propMap
     * @return
     * @throws Exception
     */
    @Override
    @TimeTrace
    public Object onMessageData(IFContext context, String txid, String msgCreDt, Object requestData, Map<String, String> propMap) throws Exception {

        TCPRequest request = new TCPRequest();

        String interfaceId = context.getInterfaceId();

        //로그에 남기기위한것
        String tcpSendMsg = null;
        String tcpRecvMsg = null;

        Map inData = (Map) requestData;
        log.info("NetworkAdaptorOutbound Request : {}", inData);

        String transformDt = DateUtils.today17();
        String outMapperType = context.getOutMapperType();
        String inMapperType = context.getInMapperType();

        Map<String, Object> retMap = Maps.newLinkedHashMap();

        //시작 로그
        String sendDt = DateUtils.today17();
        NetworkAdaptorAPI networkAdaptorAPI = null;

        String receiverId = null;

        if (embeddedNA != null) {
            log.info("EmbeddedNetworkAdaptor Detected");
            networkAdaptorAPI = embeddedNetworkAdaptorAPI;
            receiverId = context.getReceiverIds()[0];
        } else {
            log.info("RemoteNetworkAdaptor Detected");
            String dataReceiverId = ifConfig.getDataReceiverId();
            if (StringUtils.isNotEmpty(dataReceiverId)) {
                receiverId = (String) inData.get(dataReceiverId);
                networkAdaptorAPI = appContext.getBean(receiverId, NetworkAdaptorAPI.class);
            } else {
                receiverId = context.getReceiverIds()[0];
                networkAdaptorAPI = appContext.getBean(context.getReceiverIds()[0], NetworkAdaptorAPI.class);
            }
        }

        try {

            log.info("transformType " + outMapperType);

            switch (outMapperType) {
                case "MAPPER": {
                    //context check
                    if (context.getMapperGroupId() == null || context.getOutBoundMapperMessageId() == null) {
                        throw new Exception("Mapper GroupId or MessageId is null");
                    }
                    //myMessageFormBox check
                    if (myMessageFormBox == null) {
                        throw new Exception("myMessageFormBox is null");
                    }

                    MessageForm messageForm = null;
                    try {
                        messageForm = myMessageFormBox.get(context.getMapperGroupId()).get(context.getOutBoundMapperMessageId()).copidMessageForm();
                    } catch (Exception e) {
                        log.error("myMessageFormBox is null", e);
                    }

                    //messageForm check
                    if (messageForm == null) {
                        throw new Exception("messageForm not found");
                    }

                    byte[] sendBytes = mapEngine.conversionToAsciiBytes(inData, messageForm.getInContext(), messageForm.getOutContext());

                    if (context.isLengthFieldInject()) {
                        byte[] lengthByte = null;
                        int totalLength = new String(sendBytes).getBytes().length;
                        int lengthFieldValue = totalLength + context.getLengthFieldAddition();
                        log.info("totalLength : {}, lengthFieldValue : {}", totalLength, lengthFieldValue);
                        byte[] lengthFiledBytes = TypeConversion.asciiConvert((lengthFieldValue + "").getBytes(), "N", context.getLengthFieldLength());

                        ByteBuf byteBuf = Unpooled.wrappedBuffer(sendBytes);
                        byteBuf.setBytes(context.getLengthFieldPosition(), lengthFiledBytes);

                        byte[] injectSendBytes = new byte[byteBuf.readableBytes()];
                        byteBuf.getBytes(0, injectSendBytes);

                        tcpSendMsg = base64Encoder.encodeToString(injectSendBytes);
                        request.setData(tcpSendMsg);
                    } else {
                        tcpSendMsg = base64Encoder.encodeToString(sendBytes);
                        request.setData(tcpSendMsg);
                    }

                    break;
                }
                case "PARSER": {
                    //context check
                    String parserId = context.getParserId();
                    ATBParser atbParser = applicationContext.getBean(parserId, ATBParser.class);
                    byte[] sendBytes = atbParser.MapToBytes(inData);
                    tcpSendMsg = base64Encoder.encodeToString(new String(sendBytes).getBytes());
                    request.setData(tcpSendMsg);
                    break;
                }
                case "BYPASS":
                default: {
                    String srcData = (String) inData.get(THeader.DATA.key());
                    request.setData(srcData);
                    tcpSendMsg = srcData;
                }
            }
        } catch (Exception e) {
            log.error("TRANSFORM ERROR", e);
            retGenMessage(context, "TRANSFORM ERROR : " + e.getMessage(), TResult.FAIL.value(), txid, "", retMap, tcpSendMsg, tcpRecvMsg, receiverId);
            return retMap;
        }
        request.setLnk_interface_id(interfaceId);
        request.setLnk_pattern(outMapperType);
        request.setLnk_src_org(context.getSendSystemCode());
        request.setLnk_trg_org(context.getReceiveSystemCode());
        request.setLnk_transaction_id(txid);
        log.info("request : {}", request);

        try {
            String resultCd;
            String resultMsg;
            int errcnt = 0;
            log.info("tcpMode : {}", context.getProcessMode());
            switch (context.getProcessMode()) {
                case "ASYNC": {
                    //비동기 응답
                    TCPRequestAck sendAck = networkAdaptorAPI.send(request);

                    if (sendAck.getLnk_result_cd().equals("0000")) {
                        resultCd = "S";
                    } else {
                        resultCd = "F";
                        errcnt = 1;
                    }
                    retGenMessage(context, sendAck.getLnk_result_msg(), resultCd, txid, sendDt, retMap, tcpSendMsg, tcpRecvMsg, receiverId);
                    break;
                }
                case "SYNC": {
                    //동기 응답
                    TCPResponse response = networkAdaptorAPI.request(request);
                    log.info("응답 메시지 : {}", response);
                    if (response.getLnk_result_cd().equals("0000")) {
                        resultCd = "S";
                    } else {
                        resultCd = "F";
                        errcnt = 1;

                        retGenMessage(context, response.getLnk_result_msg(), resultCd, txid, sendDt, retMap, tcpSendMsg, tcpRecvMsg, receiverId);
                        return retMap;

                    }

                    tcpRecvMsg = response.getData();

                    switch (inMapperType) {
                        case "MAPPER": {
                            // response.getData() 를 MAP 으로 변환
                            MessageForm messageForm = null;
                            try {
                                messageForm = myMessageFormBox.get(context.getMapperGroupId()).get(context.getInBoundMapperMessageId()).copidMessageForm();
                            } catch (Exception e) {
                                log.error("myMessageFormBox is null", e);
                            }

                            //messageForm check
                            if (messageForm == null) {
                                throw new Exception("messageForm not found");
                            }
                            byte[] decode = base64Decoder.decode(response.getData());
                            Map convertMap = asciiEngine.conversionToMap(new String(decode, "EUC-KR").getBytes(), messageForm.getInContext(), messageForm.getOutContext());

                            retMap = convertMap;
                            break;
                        }
                        case "PARSER": {
                            //context check
                            String parserId = context.getParserId();
                            ATBParser atbParser = applicationContext.getBean(parserId, ATBParser.class);
                            byte[] decode = base64Decoder.decode(response.getData());
                            retMap = atbParser.BytesToMap(decode);
                            break;
                        }
                        case "BYPASS":
                        default: {
                            retMap.put(THeader.DATA.key(), response.getData());
                        }
                    }

                    retGenMessage(context, response.getLnk_result_msg(), resultCd, txid, sendDt, retMap, tcpSendMsg, tcpRecvMsg, receiverId);
                    break;
                }
                default: {
                    throw new Exception("TCP_MODE IS NOT DEFINED");
                }
            }

        } catch (Exception e) {
            log.error("TRANSFORM or TCP/IP MODULE CALL ERROR", e);
            retGenMessage(context, "TRANSFORM or TCP/IP MODULE CALL ERROR : " + e.getMessage(), TResult.FAIL.value(), txid, sendDt, retMap, tcpSendMsg, tcpRecvMsg, receiverId);
        }
        return retMap;
    }

    public void retGenMessage(IFContext context, String errorMsg, String resultCd, String txid, String sendDt, Map<String, Object> retMap, String sendTcpMsg, String recvTcpMsg, String receiverId) {
        LinkedHashMap<String, Object> ifResult = Maps.newLinkedHashMap();
        ifResult.put(THeader.INTERFACE_ID.key(), context.getInterfaceId());
        ifResult.put(THeader.RECEIVER_RESULT_MSG.key(), errorMsg);
        ifResult.put(THeader.RECEIVER_RESULT_CD.key(), resultCd);
        ifResult.put(THeader.TRANSACTION_ID.key(), txid);
        ifResult.put(THeader.SENDER_MSG_SEND_DT.key(), sendDt);
        ifResult.put(THeader.TCP_SEND_MSG.key(), sendTcpMsg);
        ifResult.put(THeader.TCP_RECV_MSG.key(), recvTcpMsg);
        ifResult.put(THeader.RECEIVER_ID.key(), receiverId);
        retMap.put(THeader.IF_RESULT.key(), ifResult);
    }

    @Override
    public void onMessageResult(IFContext context, String txid, String eventDt, String resultCode, String resultMessage, String dataStr, Map<String, String> propMap) throws Exception {

    }

    /**
     * WebClient를 생성 하여 리턴
     *
     * @param ifConfig
     * @param httpClient
     * @return
     */
    private WebClient getWebClient(IFConfig ifConfig, HttpClient httpClient) {
        return WebClient.builder().
                clientConnector(new ReactorClientHttpConnector(httpClient)).baseUrl(ifConfig.getAddress()).exchangeStrategies(ExchangeStrategies.builder().codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(100 * 1024 * 1024)).build()).defaultCookie("cookieKey", "cookieValue").defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).defaultUriVariables(Collections.singletonMap("url", ifConfig.getAddress())).build();
    }

}
