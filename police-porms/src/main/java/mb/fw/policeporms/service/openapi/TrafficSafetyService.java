package mb.fw.policeporms.service.openapi;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import mb.fw.policeporms.constants.ApiNameConstants;
import mb.fw.policeporms.service.CommonApiService;
import mb.fw.policeporms.spec.InterfaceSpec;
import reactor.core.publisher.Mono;

@Service
public class TrafficSafetyService implements CommonApiService {

	@Override
	public String getApiName() {
		return ApiNameConstants.서울신호등관련정보;
	}

	@Override
	public Mono<List<Map<String, Object>>> fetch(InterfaceSpec spec, WebClient webClient) {
		return webClient.get().uri(spec.getApiUrl()).retrieve().bodyToMono(JsonNode.class).map(rootNode -> {
			// 1. JSON 설정에 정의된 rootKey로 접근 (예: trafficSafetyA057PInfo)
			JsonNode contentNode = rootNode.get(spec.getApiName());
			// 2. 그 안의 "row" 배열 노드 추출
			JsonNode rowNode = contentNode.get("row");
			// 3. Jackson ObjectMapper를 사용하여 List<Map>으로 변환
			ObjectMapper mapper = new ObjectMapper();
			return mapper.convertValue(rowNode, new TypeReference<List<Map<String, Object>>>() {
			});
		});
	}
}
