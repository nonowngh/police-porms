package mb.fw.policeporms.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import mb.fw.atb.util.TransactionIdGenerator;
import mb.fw.policeporms.dto.RequestMessage;
import mb.fw.policeporms.dto.ResponseMessage;
import mb.fw.policeporms.dto.openapi.서울시신호등관련정보.TrafficSafetyResponse;
import reactor.core.publisher.Mono;

@Service
public class InterfaceCallService {

	private final WebClient interfaceWebClient;
	private final WebClient openApiWebClient;

	public InterfaceCallService(@Qualifier("interfaceWebClient") WebClient interfaceWebClient,
			@Qualifier("openApiWebClient") WebClient openApiWebClient) {
		this.interfaceWebClient = interfaceWebClient;
		this.openApiWebClient = openApiWebClient;
	}

	public Mono<ResponseMessage> sendData(String interfaceId, List<Map<String, Object>> dataList) {
		RequestMessage request = new RequestMessage();
		request.setInterfaceId(interfaceId);
		String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
		String transactionId = TransactionIdGenerator.generate(interfaceId, "", currentDate);
		request.setTransactionId(transactionId);
		request.setDataList(dataList);
		request.setDataCount(dataList.size());
		return interfaceWebClient.post().bodyValue(request).retrieve().bodyToMono(ResponseMessage.class);
	}

	public TrafficSafetyResponse getTrafficSafetyInfoWebClient(String apiUrl, String serviceKey, int pageNo,
			int numOfRows) {

		return openApiWebClient.get()
				.uri(uriBuilder -> uriBuilder.path(apiUrl).queryParam("serviceKey", serviceKey)
						.queryParam("pageNo", pageNo).queryParam("numOfRows", numOfRows).queryParam("type", "json")
						.build())
				.retrieve().bodyToMono(TrafficSafetyResponse.class).block();
	}

}
