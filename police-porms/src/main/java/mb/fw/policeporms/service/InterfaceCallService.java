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
import mb.fw.policeporms.spec.InterfaceSpec;
import reactor.core.publisher.Mono;

@Service
public class InterfaceCallService {

	private final WebClient interfaceWebClient;
	private final WebClient openApiWebClient;
	private final List<CommonApiService> services;

	public InterfaceCallService(@Qualifier("interfaceWebClient") WebClient interfaceWebClient,
			@Qualifier("openApiWebClient") WebClient openApiWebClient, List<CommonApiService> services) {
		this.interfaceWebClient = interfaceWebClient;
		this.openApiWebClient = openApiWebClient;
		this.services = services;
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

	@SuppressWarnings("unchecked")
	public Mono<List<Map<String, Object>>> callApi(InterfaceSpec spec) {
		CommonApiService service = services.stream().filter(s -> s.getApiName().equals(spec.getApiName())).findFirst()
				.orElseThrow(() -> new RuntimeException("No service found for " + spec.getApiName()));
		return (Mono<List<Map<String, Object>>>) service.fetch(spec, openApiWebClient);
	}
}
