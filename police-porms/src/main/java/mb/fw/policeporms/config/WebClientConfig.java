package mb.fw.policeporms.config;

import java.net.ConnectException;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.TimeoutException;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mb.fw.policeporms.constants.InterfaceAuthConstants;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

@Profile("sender")
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "web.client", ignoreUnknownFields = true)
public class WebClientConfig {

	private String interfaceApiUrl;

	@Bean("interfaceWebClient")
	WebClient esbWebClient() {
		// 대용량 처리를 위한 메모리 제한 해제 (200MB)
	    ExchangeStrategies strategies = ExchangeStrategies.builder()
	            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(200 * 1024 * 1024))
	            .build();
	    
	    HttpClient httpClient = HttpClient.create()
	            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) // 연결 시도 시간 (10초)
	            .responseTimeout(Duration.ofMinutes(15))            // 전체 응답 대기 시간 (15분으로 상향)
	            .doOnConnected(conn -> conn
	                // 개별 패킷 사이의 읽기/쓰기 제한 시간을 0(무제한) 혹은 매우 길게 설정
	                .addHandlerLast(new ReadTimeoutHandler(0)) 
	                .addHandlerLast(new WriteTimeoutHandler(0)));
	    return WebClient.builder()
	            .baseUrl(interfaceApiUrl)
	            .exchangeStrategies(strategies) // 메모리 전략 적용
	            .clientConnector(new ReactorClientHttpConnector(httpClient))
	            .defaultHeader(InterfaceAuthConstants.AUTH_HEADER, InterfaceAuthConstants.AUTH_KEY)
	            .build();
	}

//	@Bean("openApiWebClient")
//	WebClient openApiWebClient() {
//		HttpClient httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 연결 타임아웃
//				.responseTimeout(Duration.ofSeconds(5)) // 응답 타임아웃
//				.doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(5))
//						.addHandlerLast(new WriteTimeoutHandler(5)));
//		return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient))
//				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
//	}

	@Bean("openApiWebClient")
	WebClient openApiWebClient() {
		HttpClient httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
				.responseTimeout(Duration.ofSeconds(30)) // 1GB 대응을 위해 넉넉히
				.doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(300))
						.addHandlerLast(new WriteTimeoutHandler(300)));

		return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient))
				// --- 재처리(Retry) 필터 추가 시작 ---
				.filter((request, next) -> next.exchange(request).flatMap(response -> {
					// 5xx 에러 발생 시 에러로 강제 전환하여 재시도 유도
					if (response.statusCode().is5xxServerError()) {
						return response.createException().flatMap(Mono::error);
					}
					return Mono.just(response);
				}).retryWhen(
						Retry.backoff(3, Duration.ofSeconds(2)).jitter(0.75).filter(throwable -> isRetryable(throwable))
								.doBeforeRetry(retrySignal -> log.warn("OpenAPI 호출 재시도 중... 횟수: {}/3, 사유: {}",
										retrySignal.totalRetries() + 1, retrySignal.failure().getMessage()))))
				// --- 재처리(Retry) 필터 추가 끝 ---
				.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
	}

	// 재시도 대상 판별 메서드
	private boolean isRetryable(Throwable ex) {
		// 1. 타임아웃 2. 서버에러(5xx) 3. 커넥션 거부 등 네트워크 오류 시 재시도
		return ex instanceof TimeoutException || ex instanceof WebClientResponseException
				|| ex instanceof ConnectException;
	}
}
