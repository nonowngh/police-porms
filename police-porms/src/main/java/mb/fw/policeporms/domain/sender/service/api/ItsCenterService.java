package mb.fw.policeporms.domain.sender.service.api;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import mb.fw.policeporms.common.annotation.SenderComponent;
import mb.fw.policeporms.common.constant.ApiResponseKeys;
import mb.fw.policeporms.common.constant.ApiType;
import mb.fw.policeporms.common.spec.InterfaceSpec;
import mb.fw.policeporms.common.utils.InterfaceEncryptUtils;
import mb.fw.policeporms.common.utils.LoggingUtils;
import mb.fw.policeporms.common.utils.WebClientUtils;
import mb.fw.policeporms.domain.sender.service.base.AbstractApiService;
import reactor.core.publisher.Mono;

@Slf4j
@SenderComponent
public class ItsCenterService extends AbstractApiService {

	private static final String PARAM_COORDINATE_LIST = "coordinateList";
	private static final String PARAM_MIN_X = "minX";
	private static final String PARAM_MIN_Y = "minY";
	private static final String PARAM_MAX_X = "maxX";
	private static final String PARAM_MAX_Y = "maxY";

	protected ItsCenterService(ObjectMapper objectMapper, WebClient openApiWebClient) {
		super(objectMapper, openApiWebClient);
	}

	@Override
	public ApiType getApiType() {
		return ApiType.ITS_CENTER;
	}

	@Override
	public int fetchAndSave(InterfaceSpec spec, Path tempFile, String transactionId) {
		int totalSaved = 0;
		boolean isEncrypt = spec.isDataEncrypt();

		Map<String, Object> processedParams = spec.getAdditionalParams() != null 
				? new HashMap<>(spec.getAdditionalParams()) 
				: new HashMap<>();

		try (OutputStream fos = Files.newOutputStream(tempFile, StandardOpenOption.CREATE);
				BufferedOutputStream bos = new BufferedOutputStream(fos)) {
			
			OutputStream finalOut = bos;
			
			if (isEncrypt) {
				finalOut = InterfaceEncryptUtils.createFileEncryptOutputStream(finalOut);
			}

			try (GZIPOutputStream gzos = new GZIPOutputStream(finalOut);
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzos, StandardCharsets.UTF_8))) {
				
				Object coordinateListObj = processedParams.remove(PARAM_COORDINATE_LIST);

				// 다중 좌표(배열) 처리
				if (coordinateListObj instanceof List) {
					List<?> coordinateList = (List<?>) coordinateListObj;
					
					log.info("[{}] ITS API 다중 좌표 배열 처리 시작 (총 {}개 권역)", spec.getInterfaceId(), coordinateList.size());
					
					for (int i = 0; i < coordinateList.size(); i++) {
						Object coordObj = coordinateList.get(i);
						
						if (coordObj instanceof Map) {
							@SuppressWarnings("unchecked")
							Map<String, Object> coord = (Map<String, Object>) coordObj;
							
							try {
								if (!coord.containsKey(PARAM_MIN_X) || !coord.containsKey(PARAM_MIN_Y) 
										|| !coord.containsKey(PARAM_MAX_X) || !coord.containsKey(PARAM_MAX_Y)) {
									log.warn("[{}] {}번째 좌표 정보 누락, skip", spec.getInterfaceId(), i + 1);
									continue;
								}

								// 정제된 Map에 현재 루프의 좌표값 주입
								processedParams.put(PARAM_MIN_X, coord.get(PARAM_MIN_X));
								processedParams.put(PARAM_MIN_Y, coord.get(PARAM_MIN_Y));
								processedParams.put(PARAM_MAX_X, coord.get(PARAM_MAX_X));
								processedParams.put(PARAM_MAX_Y, coord.get(PARAM_MAX_Y));
								
								// 현재 호출에서 가져온 건수 저장
								int currentSaved = processApiCall(spec, writer, processedParams);
								// 총 누적 건수에 합산
								totalSaved += currentSaved;
								
								log.info("[{}] 배열 [{}/{}] 처리 완료 -> 이번 적재: {}건 / 총 누적 적재: {}건", 
										spec.getInterfaceId(), i + 1, coordinateList.size(), currentSaved, totalSaved);
								
							} catch (Exception e) {
								log.error("[{}] {}번째 좌표 API 호출/처리 중 오류 발생: {}", spec.getInterfaceId(), i + 1, e.getMessage());
							}
						}
					}
				} 
				// 단일 파라미터 처리
				else {
					log.info("[{}] ITS API 단일 파라미터 처리 시작", spec.getInterfaceId());
					
					int currentSaved = processApiCall(spec, writer, processedParams);
					totalSaved += currentSaved;
					
					log.info("[{}] 단일 파라미터 처리 완료 -> 총 누적 적재: {}건", spec.getInterfaceId(), totalSaved);
				}

				LoggingUtils.printWriteFileComplete(transactionId, totalSaved);
			}

		} catch (Exception e) {
			log.error("파일 처리 중 오류 -> ", e);
			throw new RuntimeException("ITS API 파일 처리 중 치명적 오류 발생", e);
		}

		return totalSaved;
	}

	private int processApiCall(InterfaceSpec spec, BufferedWriter writer, Map<String, Object> processedParams) throws Exception {
		JsonNode root = callApi(spec, processedParams);
		
		if (root == null || root.isEmpty()) {
			throw new RuntimeException("[" + spec.getInterfaceId() + "] API 응답 데이터가 비어있습니다.");
		}

		JsonNode headerNode = root.path(ApiResponseKeys.ITS_CENTER_HEADER.getValue());
		JsonNode bodyNode = root.path(ApiResponseKeys.ITS_CENTER_BODY.getValue());
		int totalCount = getTotalSize(bodyNode);
		
		log.debug("'{}' api response result : {}, total-count : {}", spec.getApiServiceId(), headerNode.toString(), totalCount);
		
		if (totalCount == 0) {
			return 0;
		}
		
		String resultCode = getResult(headerNode).asText();
		if (!ApiResponseKeys.ITS_CENTER_RESULT_CODE_SUCCESS.getValue().equals(resultCode)) {
			throw new RuntimeException("[" + spec.getInterfaceId() + "] API 연동 오류 발생 - 응답코드: " + headerNode.asText());
		}
		
		JsonNode rowNode = bodyNode.path(ApiResponseKeys.ITS_CENTER_ITEMS.getValue());
		if (!rowNode.isMissingNode() && rowNode.isArray()) {
			List<Map<String, Object>> rows = objectMapper.convertValue(rowNode,
					new TypeReference<List<Map<String, Object>>>() {});
			
			writeRowsToWriter(writer, rows);
			return rows.size();
		}
		
		return 0;
	}

	private int getTotalSize(JsonNode bodyNode) {
		return bodyNode.path(ApiResponseKeys.ITS_CENTER_TOTAL_COUNT.getValue()).asInt(0);
	}

	private JsonNode getResult(JsonNode headerNode) {
		return headerNode.path(ApiResponseKeys.ITS_CENTER_RESULT_CODE.getValue());
	}

	private JsonNode callApi(InterfaceSpec spec, Map<String, Object> processedParams) {
		String apiPath = String.format("/%s", spec.getApiServiceId());
		
		UriComponentsBuilder builder = WebClientUtils.appendQueryParams(spec, apiPath);
		
		if (processedParams != null) {
			if (spec.getAdditionalParams() != null) {
				for (String key : spec.getAdditionalParams().keySet()) {
					builder.replaceQueryParam(key); 
				}
			}
			for (Map.Entry<String, Object> entry : processedParams.entrySet()) {
				builder.queryParam(entry.getKey(), entry.getValue());
			}
		}

		java.net.URI finalUri = builder.build(false).encode(StandardCharsets.UTF_8).toUri();
		log.info("ITS 요청 API URL: {}", finalUri.toString());

		return openApiWebClient.get().uri(finalUri).retrieve()
				.onStatus(status -> status.isError(), response -> {
					return response.bodyToMono(String.class).flatMap(body -> {
						log.error("API 호출 에러 발생! 응답 바디: {}", body);
						return Mono.error(new RuntimeException("API HTTP 상태 코드 오류(" + body + ")"));
					});
				}).bodyToMono(String.class).map(res -> {
					if (ObjectUtils.isEmpty(res)) {
						throw new RuntimeException("API 응답 본문이 비어있습니다.");
					}
					try {
						return objectMapper.readTree(res);
					} catch (Exception e) {
						log.error("JSON 파싱 오류 발생. 원본 응답: {}", res);
						throw new RuntimeException("JSON 데이터 파싱 오류", e);
					}
				}).block();
	}
}