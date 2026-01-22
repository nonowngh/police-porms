package mb.fw.atb.strategy.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.indigo.indigomq.memory.LRUMap;
import com.mb.indigo2.springsupport.AdaptorConfigBean;
import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTrace;
import mb.fw.atb.config.IFConfig;
import mb.fw.atb.config.Specifications;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.enums.THeader;
import mb.fw.atb.enums.TResult;
import mb.fw.atb.job.com.ToJMSData;
import mb.fw.atb.model.OnSignalInfo;
import mb.fw.atb.strategy.ATBStrategy;
import mb.fw.atb.util.DateUtils;
import mb.fw.atb.util.ESBProductEncryption;
import mb.fw.atb.util.crypto.kisa.KISA_SEED_CBC;
import mb.fw.atb.util.crypto.kisa.KISA_SEED_ECB;
import mb.fw.atb.util.police.MessageCryptoSeed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 경찰청 무인발급기 - WAS간 연계 프로바이더
 * Adaptor(Remote Provider) -> Queue -> JMS Adaptor(PoliceIssuingStrategy) -> WAS
 */
@Component(value = "PoliceIssuingStrategy")
@Slf4j
public class PoliceIssuingStrategy extends ATBStrategy {

    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    IFConfig ifConfig;

    HttpClient httpClient;

    WebClient client;

    LRUMap<String, HttpStatus> responseMap = new LRUMap<>(1000000);

    @Autowired
    private AdaptorConfigBean adaptorConfigBean;

    @Autowired(required = false)
    private JmsTemplate jmsTemplate;

    ESBProductEncryption productEncryption = new ESBProductEncryption();
    String seedKey;
    String initVector;

    Base64.Decoder decoder = Base64.getDecoder();
    Base64.Encoder encoder = Base64.getEncoder();

    MessageCryptoSeed policeSeed = null;

    @PostConstruct
    public void init() {
        httpClient = HttpClient.create().wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000).responseTimeout(Duration.ofSeconds(30)).doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS)).addHandlerLast(new WriteTimeoutHandler(60, TimeUnit.SECONDS)));
        client = getWebClient(ifConfig, httpClient);

        /**
         * SEED 암호화를 위한 키를 초기화한다.
         */
        String encryptionKey = ifConfig.getEncryptionKey();
        String initVector = ifConfig.getInitVector();
        if (encryptionKey != null) {
            this.seedKey = ESBProductEncryption.decryptString(encryptionKey);
        }
        if (initVector != null) {
            this.initVector = ESBProductEncryption.decryptString(initVector);
        }

        this.policeSeed = new MessageCryptoSeed(seedKey);
    }

    @Override
    public Specifications specifications() {
        return null;
    }

    @Override
    public OnSignalInfo onSignal(IFContext context, String txid, String msgCreDt) throws Exception {
        return null;
    }

    @Override
    public OnSignalInfo onSignalRetry(IFContext context, String txid, String eventDt, Map<String, String> propMap) throws Exception {
        return null;
    }

    @Override
    @TimeTrace
    public Object onMessageData(IFContext context, String txid, String msgCreDt, Object data, Map<String, String> propMap) throws Exception {
        //시작 로그
        String sendDt = DateUtils.today17();

        Map dataMap = null;

        if (data instanceof String) {
            dataMap = objectMapper.readValue((String) data, Map.class);
        } else {
            dataMap = (Map) data;
        }


        /** 수신되는 json 규격
         {
         "data": "BASE64 String(SEED로 암호화된 Byte Array)"
         }
         **/

        /** 응답 json 규격
         {
         "data": "BASE64 String(SEED로 암호화된 Byte Array)"
         "ifResult" : {
         "interfaceId" : "인터페이스ID",
         "transactionId" : "트랜잭션ID",
         "receiverResultCd" : "응답코드",
         "receiverResultMsg" : "응답메시지"
         }
         }
         **/

        String dataEnc = (String) dataMap.get("data");
        log.info("BASE64 DECODE : {} bytes", dataEnc.getBytes().length);
        byte[] decode = decoder.decode(dataEnc.getBytes());
        log.info("SEED_DECRYPT : {} bytes", decode.length);
        try {
            switch (ifConfig.getSeedOpMode()) {
                case "ECB":
                    decode = KISA_SEED_ECB.SEED_ECB_Decrypt(seedKey.getBytes(), decode, 0, decode.length);
                    break;
                case "CBC":
                    decode = KISA_SEED_CBC.SEED_CBC_Decrypt(seedKey.getBytes(), initVector.getBytes(), decode, 0, decode.length);
                    break;
                case "POLICE":
                    decode = policeSeed.decode(decode);
                    break;
            }
        } catch (Exception e) {
            log.error("OUTBOUND SEED_Encrypt_Error", e);
            LinkedHashMap returnMap = Maps.newLinkedHashMap();
            String msgRcvDt = DateUtils.today17();
            HttpStatus status = responseMap.get(txid);

            propMap.put(THeader.RECEIVER_ID.key(), ifConfig.getId());
            propMap.put(THeader.RECEIVER_STRATEGY.key(), context.getStrategy());
            propMap.put(THeader.RECEIVER_MSG_RECV_DT.key(), msgRcvDt);
            propMap.put(THeader.RECEIVER_ADAPTOR_NAME.key(), adaptorConfigBean.getAdaptorName());
            propMap.put(THeader.RECEIVER_RESULT_CD.key(), TResult.FAIL.value());

            propMap.put(THeader.RECEIVER_RESULT_MSG.key(), "OUTBOUND SEED_Encrypt_Error");

            returnMap.put(THeader.IF_RESULT.key(), propMap);
            responseMap.remove(txid);
            return returnMap;
        }


        if (decode == null) {
            log.error("SEED_DECRYPT FAIL");
            LinkedHashMap returnMap = Maps.newLinkedHashMap();
            propMap.put(THeader.RECEIVER_ID.key(), ifConfig.getId());
            propMap.put(THeader.RECEIVER_STRATEGY.key(), context.getStrategy());
            propMap.put(THeader.RECEIVER_MSG_RECV_DT.key(), DateUtils.today17());
            propMap.put(THeader.RECEIVER_ADAPTOR_NAME.key(), adaptorConfigBean.getAdaptorName());
            propMap.put(THeader.RECEIVER_RESULT_CD.key(), TResult.FAIL.value());
            propMap.put(THeader.RECEIVER_RESULT_MSG.key(), "SEED_DECRYPT FAIL");

            returnMap.put(THeader.IF_RESULT.key(), propMap);
            return returnMap;
        }

        log.info("KISA_SEED_DECRYPT AFTER : {} bytes", decode.length);
        String xml = new String(decode);
        log.info("KISA_SEED_DECRYPT AFTER : {} xml", xml.length());
        log.info("KISA_SEED_DECRYPT AFTER : {} xml", xml.toString());   // added

        MediaType type = MediaType.APPLICATION_XML;

        Mono<String> result = client
                .post()
                .uri(context.getContextPath())
                .headers(httpHeaders -> {
                })
                .accept(type)
                .bodyValue(xml)
                .exchangeToMono(clientResponse -> {
                    responseMap.put(txid, clientResponse.statusCode());
                    Mono<String> response = clientResponse.bodyToMono(String.class);
                    return response;
                });

        String resultStr = result.block();

        log.info("receive : {}", resultStr);

        byte[] seedEncBytes = null;

        //resultStr 받아서 seed로 다시 암호화를 하고 map에 담아서 리턴한다.
        try {
            switch (ifConfig.getSeedOpMode()) {
                case "ECB":
                    seedEncBytes = KISA_SEED_ECB.SEED_ECB_Encrypt(seedKey.getBytes(), resultStr.getBytes(), 0, resultStr.getBytes().length);
                    break;
                case "CBC":
                    seedEncBytes = KISA_SEED_CBC.SEED_CBC_Encrypt(seedKey.getBytes(), initVector.getBytes(), resultStr.getBytes(), 0, resultStr.getBytes().length);
                    break;
                case "POLICE":
                    seedEncBytes = policeSeed.encode(resultStr);
                    break;
            }
        } catch (Exception e) {
            log.error("INBOUND SEED_Encrypt_Error", e);
            LinkedHashMap returnMap = Maps.newLinkedHashMap();
            String msgRcvDt = DateUtils.today17();
            HttpStatus status = responseMap.get(txid);

            propMap.put(THeader.RECEIVER_ID.key(), ifConfig.getId());
            propMap.put(THeader.RECEIVER_STRATEGY.key(), context.getStrategy());
            propMap.put(THeader.RECEIVER_MSG_RECV_DT.key(), msgRcvDt);
            propMap.put(THeader.RECEIVER_ADAPTOR_NAME.key(), adaptorConfigBean.getAdaptorName());
            propMap.put(THeader.RECEIVER_RESULT_CD.key(), TResult.FAIL.value());

            propMap.put(THeader.RECEIVER_RESULT_MSG.key(), "INBOUND SEED_Encrypt_Error");

            returnMap.put(THeader.IF_RESULT.key(), propMap);
            responseMap.remove(txid);
            return returnMap;
        }


        String base64Data = encoder.encodeToString(seedEncBytes);
        LinkedHashMap returnMap = Maps.newLinkedHashMap();
        returnMap.put("data", base64Data);

        String msgRcvDt = DateUtils.today17();
        HttpStatus status = responseMap.get(txid);

        propMap.put(THeader.RECEIVER_ID.key(), ifConfig.getId());
        propMap.put(THeader.RECEIVER_STRATEGY.key(), context.getStrategy());
        propMap.put(THeader.RECEIVER_MSG_RECV_DT.key(), msgRcvDt);
        propMap.put(THeader.RECEIVER_ADAPTOR_NAME.key(), adaptorConfigBean.getAdaptorName());

        if (status.is2xxSuccessful()) {
            propMap.put(THeader.RECEIVER_RESULT_CD.key(), TResult.SUCCESS.value());
        } else {
            propMap.put(THeader.RECEIVER_RESULT_CD.key(), TResult.FAIL.value());
        }

        propMap.put(THeader.RECEIVER_RESULT_MSG.key(), "HTTP STATUS : " + status.value() + " , " + status.name());

        returnMap.put(THeader.IF_RESULT.key(), propMap);

        responseMap.remove(txid);

        return returnMap;
    }

    @Override
    public void onMessageResult(IFContext context, String txid, String eventDt, String resultCode, String
            resultMessage, String dataStr, Map<String, String> propMap) throws Exception {

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
