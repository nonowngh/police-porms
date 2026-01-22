package mb.fw.atb.strategy.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import mb.fw.atb.config.Specifications;
import mb.fw.atb.enums.THeader;
import mb.fw.atb.enums.TResult;
import mb.fw.atb.config.IFConfig;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.model.OnSignalInfo;
import mb.fw.atb.strategy.ATBStrategy;
import mb.fw.atb.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component(value = "StandardRSTRCPOSTStrategy")
@Slf4j
public class StandardRSTRCPOSTStrategy extends ATBStrategy {

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

    @PostConstruct
    public void init() {
        httpClient = HttpClient.create().wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000).responseTimeout(Duration.ofSeconds(30)).doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(1, TimeUnit.HOURS)).addHandlerLast(new WriteTimeoutHandler(1, TimeUnit.HOURS)));
        client = getWebClient(ifConfig, httpClient);
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

    /**
     *
     */
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

        MediaType type = MediaType.APPLICATION_JSON;

        Mono<Map> result = client
                .post()
                .uri(context.getContextPath())
                .headers(httpHeaders -> {
                })
                .accept(type)
                .bodyValue(dataMap)
                .exchangeToMono(clientResponse -> {
                    responseMap.put(txid, clientResponse.statusCode());
                    Mono<Map> response = clientResponse.bodyToMono(Map.class);
                    return response;
                });

        return result.map(map -> {
            String msgRcvDt = DateUtils.today17();
            HttpStatus status = responseMap.get(txid);
            Map atbMap = Maps.newLinkedHashMap();
            Map<String, String> retPropMap = Maps.newLinkedHashMap();

            int errCount = 0;

            retPropMap.put(THeader.INTERFACE_ID.key(), context.getInterfaceId());
            retPropMap.put(THeader.TRANSACTION_ID.key(), txid);
            retPropMap.put(THeader.SENDER_ID.key(), ifConfig.getId());
            retPropMap.put(THeader.SENDER_MSG_CREATE_DT.key(), msgCreDt);
            retPropMap.put(THeader.SENDER_STRATEGY.key(), context.getStrategy());
            retPropMap.put(THeader.SENDER_MSG_SEND_DT.key(), sendDt);
            retPropMap.put(THeader.SENDER_ADAPTOR_NAME.key(), adaptorConfigBean.getAdaptorName());
            retPropMap.put(THeader.SENDER_DATA_COUNT.key(), String.valueOf(1));
            retPropMap.put(THeader.RECEIVER_ID.key(), ifConfig.getId());
            retPropMap.put(THeader.RECEIVER_STRATEGY.key(), context.getStrategy());
            retPropMap.put(THeader.RECEIVER_MSG_RECV_DT.key(), msgRcvDt);
            retPropMap.put(THeader.RECEIVER_ADAPTOR_NAME.key(), adaptorConfigBean.getAdaptorName());
            if (status.is2xxSuccessful()) {
                retPropMap.put(THeader.RECEIVER_RESULT_CD.key(), TResult.SUCCESS.value());
            } else {
                retPropMap.put(THeader.RECEIVER_RESULT_CD.key(), TResult.FAIL.value());
                errCount = 1;
            }
            retPropMap.put(THeader.RECEIVER_RESULT_MSG.key(), "HTTP STATUS : " + status.value() + " , " + status.name());

            map.put(THeader.IF_RESULT.key(), retPropMap);

            String reqMessageJson = null;
            String resMessageJson = null;
            String propertyJson = null;
            try {
                reqMessageJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
                resMessageJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
                propertyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(retPropMap);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            responseMap.remove(txid);

            return map;
        }).block();
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
