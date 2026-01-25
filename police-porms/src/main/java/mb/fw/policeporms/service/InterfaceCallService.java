package mb.fw.policeporms.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import mb.fw.atb.util.TransactionIdGenerator;
import mb.fw.policeporms.dto.RequestMessage;
import mb.fw.policeporms.dto.ResponseMessage;
import reactor.core.publisher.Mono;

@Component
public class InterfaceCallService {

	private final WebClient webClient;

	public InterfaceCallService(WebClient interfaceWebClient) {
		this.webClient = interfaceWebClient;
	}

	public Mono<ResponseMessage> sendOpenApiData(String interfaceId, List<Map<String, Object>> dataList) {

		RequestMessage request = new RequestMessage();
		request.setInterfaceId(interfaceId);
		String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
		String transactionId = TransactionIdGenerator.generate(interfaceId, "", currentDate);
		request.setTransactionId(transactionId);
		request.setDataList(dataList);
		request.setDataCount(dataList.size());

		return webClient.post().uri("/api/interface").bodyValue(request).retrieve().bodyToMono(ResponseMessage.class);
	}

}
