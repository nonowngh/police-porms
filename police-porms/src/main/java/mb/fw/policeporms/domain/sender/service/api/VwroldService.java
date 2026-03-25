package mb.fw.policeporms.domain.sender.service.api;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;
import mb.fw.policeporms.common.annotation.SenderComponent;
import mb.fw.policeporms.common.constant.ApiResponseKeys;
import mb.fw.policeporms.common.constant.ApiType;
import mb.fw.policeporms.common.spec.InterfaceSpec;
import mb.fw.policeporms.common.utils.LoggingUtils;
import mb.fw.policeporms.common.utils.WebClientUtils;
import mb.fw.policeporms.domain.sender.service.base.AbstractApiService;
import reactor.core.publisher.Mono;

@Slf4j
@SenderComponent
public class VwroldService extends AbstractApiService {

	private static final int DEFAULT_TIMEOUT_SECONDS = 30;
	private static final String PARAM_START_INDEX = "startindex";
	private static final String RESPONSE_ERROR_CODE = "code";
	private static final String RESPONSE_ERROR_TEXT = "text";

	protected VwroldService(ObjectMapper objectMapper, WebClient openApiWebClient) {
		super(objectMapper, openApiWebClient);
	}

	@Override
	public ApiType getApiType() {
		return ApiType.V_WORLD;
	}

	@Override
	public int fetchAndSave(InterfaceSpec spec, Path tempFile, String transactionId) {
		int totalSaved = 0;
		int page = 1;
		int fetchSize = spec.getApiRequestFetchSize();
		int totalCount = -1;
		
		try (OutputStream fos = Files.newOutputStream(tempFile, StandardOpenOption.CREATE);
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				GZIPOutputStream gzos = new GZIPOutputStream(bos);
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzos, StandardCharsets.UTF_8))) {
			
			while (true) {
				// 페이지 번호 전달
				JsonNode root = fetchPageFromApi(spec, page);
				
				if (root == null || !root.fieldNames().hasNext()) {
					log.warn("[{}] Empty response from API at page {}", spec.getInterfaceId(), page);
					break;
				}
				
				// 에러 체크
				if(root.has(ApiResponseKeys.V_WORLD_RESULT_EXCEPTIONS.getValue())) {
					JsonNode exceptionsNode = root.path(ApiResponseKeys.V_WORLD_RESULT_EXCEPTIONS.getValue());
					if (exceptionsNode.isArray() && exceptionsNode.size() > 0) {
						JsonNode firstException = exceptionsNode.get(0);
						String resultExpCode = firstException.path(RESPONSE_ERROR_CODE).asText();
						String resultExptext = firstException.path(RESPONSE_ERROR_TEXT).asText();
						log.error("[{}] API error code: {}, text: {}, at page {}", spec.getInterfaceId(), resultExpCode, resultExptext, page);
					} else {
						log.warn("[{}] API error code information Nothing", spec.getInterfaceId());
					}
					break;
				}
				
				// 총 갯수 저장
				if (totalCount == -1) {
					totalCount = getTotalSize(root);
				}

				// 데이터 추출 및 파일 기록
				if(!root.has(ApiResponseKeys.V_WORLD_FEATURES.getValue())) {
					log.warn("[{}] Empty response features from API at page {}", spec.getInterfaceId(), page);
					break;
				}
				
				JsonNode rowNode = makeJsonFlattener(root);
				
				if (rowNode != null && rowNode.isArray()) {
					List<Map<String, Object>> rows = objectMapper.convertValue(rowNode,
							new TypeReference<List<Map<String, Object>>>() {
							});

					// 마지막 페이지 초과 데이터 절삭 로직
					if (totalCount > 0 && (totalSaved + rows.size()) > totalCount) {
						int allowedSize = totalCount - totalSaved;
						if (allowedSize > 0) {
							log.info("[{}] 마지막 페이지 초과 데이터 절삭: 수신 {}건 -> 저장 {}건으로 조정", 
									spec.getInterfaceId(), rows.size(), allowedSize);
							rows = rows.subList(0, allowedSize);
						} else {
							rows.clear();
						}
					}

					// 파일 적재 및 오차 보정 로직
					if (!rows.isEmpty()) {
						writeRowsToWriter(writer, rows);
						totalSaved += rows.size();
						
						// 마지막 페이지(수신 데이터가 fetchSize보다 적음)인데 totalCount와 다르다면 덮어씌움
						if (rows.size() < fetchSize && totalSaved != totalCount) {
							log.debug("[{}] API 응답 총 건수({})와 실제 데이터 건수({}) 불일치. 실제 수집 건수로 보정합니다.", 
									spec.getInterfaceId(), totalCount, totalSaved);
							totalCount = totalSaved;
						}
						
						LoggingUtils.printWriteFileProgress(transactionId, rows.size(), totalSaved, totalCount);
					}

					// 종료 조건 검사
					if (rows.size() < fetchSize || totalSaved >= totalCount) {
						break;
					}
				} else {
					break;
				}
				page++;

				if (!spec.isLoopCall())
					break;
			}
			
			// 루프 무사 종료 후 파일 저장 완료 로그 출력
			LoggingUtils.printWriteFileComplete(transactionId, totalSaved);
			
		} catch (Exception e) { 
			// 예외 발생 시 임시 파일 삭제 등 안전장치
			log.error("[{}] 데이터 처리 중 오류 발생, 생성 중인 임시 파일 삭제 시도 -> ", spec.getInterfaceId(), e);
			try {
				Files.deleteIfExists(tempFile);
			} catch (IOException ex) {
				log.error("[{}] 임시 파일 삭제 실패: {}", spec.getInterfaceId(), tempFile, ex);
			}
			throw new RuntimeException("API 데이터 적재 중 오류 발생", e);
		}
		return totalSaved;
	}

	private int getTotalSize(JsonNode serviceBody) {
		return serviceBody.path(ApiResponseKeys.V_WORLD_TOTAL_COUNT.getValue()).asInt();
	}
	
	private JsonNode makeJsonFlattener(JsonNode root) {
		try {
			JsonNode featuresNode = root.get(ApiResponseKeys.V_WORLD_FEATURES.getValue());
			
			if (featuresNode == null || !featuresNode.isArray()) {
				log.warn("features 필드가 없거나 배열(Array)이 아닙니다. 빈 데이터를 반환합니다.");
				return new ObjectMapper().createArrayNode();
			}
			
			ObjectMapper mapper = new ObjectMapper();
			ArrayNode newFeaturesArray = mapper.createArrayNode();

			for (JsonNode feature : featuresNode) {
				ObjectNode flattenedFeature = mapper.createObjectNode();
				Iterator<Map.Entry<String, JsonNode>> fields = feature.fields();

				while (fields.hasNext()) {
					Map.Entry<String, JsonNode> field = fields.next();
					String key = field.getKey();
					JsonNode value = field.getValue();

					if (ApiResponseKeys.V_WORLD_FEATURE_PROPERTIES.getValue().equals(key)) {
						if (value != null && value.isObject()) {
							Iterator<Map.Entry<String, JsonNode>> props = value.fields();
							while (props.hasNext()) {
								Map.Entry<String, JsonNode> prop = props.next();
								flattenedFeature.set(prop.getKey(), prop.getValue());
							}
						}
					} else if(ApiResponseKeys.V_WORLD_FEATURE_GEOMETRY.getValue().equals(key)) {
						if (value != null && !value.isNull()) {
							flattenedFeature.put(key, value.toString());
						} else {
							flattenedFeature.set(key, value);
						}
					} else {
						flattenedFeature.set(key, value);
					}
				}
				newFeaturesArray.add(flattenedFeature);
			}

			return newFeaturesArray;
			
		} catch (Exception e) {
			throw new RuntimeException("JSON 변환 오류.", e);
		}
	}

	private JsonNode fetchPageFromApi(InterfaceSpec spec, int page) {
		String apiPath = String.format("/%s", spec.getApiServiceId());
		
		UriComponentsBuilder builder = WebClientUtils.appendQueryParams(spec, apiPath);
		Map<String, Object> additionalParams = spec.getAdditionalParams();
		if (additionalParams != null) {
			if (additionalParams.containsKey(PARAM_START_INDEX)) {
				builder.replaceQueryParam(PARAM_START_INDEX, page);
			}
		}

		int timeoutSeconds = spec.getApiRequestTimeoutSeconds() > 0 ? spec.getApiRequestTimeoutSeconds() : DEFAULT_TIMEOUT_SECONDS;

		return openApiWebClient.get().uri(builder.build().toUri()).retrieve()
				.onStatus(status -> status.isError(), response -> {
					return response.bodyToMono(String.class).flatMap(body -> {
						log.error("API 호출 에러 발생! 응답 바디: {}", body);
						return Mono.error(new RuntimeException("API 응답 오류(" + body + ")"));
					});
				}).bodyToMono(String.class).map(res -> {
					if (res.trim().startsWith("<")) {
						log.error("응답 메시지 포맷 XML : {}", res);
						throw new RuntimeException("API 서버로부터 XML 에러 메시지 수신");
					}
					try {
						return objectMapper.readTree(res);
					} catch (Exception e) {
						throw new RuntimeException("JSON 파싱 오류", e);
					}
				}).block(Duration.ofSeconds(timeoutSeconds)); // 무한 대기 방지
	}
}