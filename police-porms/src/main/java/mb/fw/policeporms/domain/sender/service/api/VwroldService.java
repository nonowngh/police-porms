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
				GZIPOutputStream gzos = new GZIPOutputStream(bos); // 압축 레이어 추가
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzos, StandardCharsets.UTF_8))) {
			while (true) {
				int start = ((page - 1) * fetchSize) + 1;
				int end = page * fetchSize;
				// api 호출
				JsonNode root = fetchPageFromApi(spec, page);
				if (root == null || !root.fieldNames().hasNext()) {
					log.warn("[{}] Empty response from API at page {}", spec.getInterfaceId(), page);
					break;
				}
				
				// 에러 체크 (exceptions가 존재하면 중단)
				if(root.has(ApiResponseKeys.V_WORLD_RESULT_EXCEPTIONS.getValue())) {
					JsonNode exceptionsNode = root.path(ApiResponseKeys.V_WORLD_RESULT_EXCEPTIONS.getValue());
					if (exceptionsNode.isArray() && exceptionsNode.size() > 0) {
		                JsonNode firstException = exceptionsNode.get(0);
		                String resultExpCode = firstException.path("code").asText();
		                String resultExptext = firstException.path("text").asText();
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
				// V-World 응답 데이터 평탄화
				JsonNode rowNode = makeJsonFlattener(root);
				
				log.debug("'{}' api response result : {}, total-count : {}", spec.getApiServiceId(),
						rowNode.toString(), getTotalSize(root));
				
				if (rowNode != null && rowNode.isArray()) {
					List<Map<String, Object>> rows = objectMapper.convertValue(rowNode,
							new TypeReference<List<Map<String, Object>>>() {
							});

					// 파일 적재 (Gzip 스트림에 작성됨)
					writeRowsToWriter(writer, rows);
					totalSaved += rows.size();
					LoggingUtils.printWriteFileProgress(transactionId, rows.size(), totalSaved, totalCount);
					log.debug("[{}] {} records saved to file (current:{}/total:{})", spec.getInterfaceId(), rows.size(),
							totalSaved, totalCount);

					if (rows.size() < fetchSize || totalSaved >= totalCount)
						break;
				} else {
					break;
				}
				page++;

				if (!spec.isLoopCall())
					break;
			}
		} catch (IOException e) {
			log.error("파일 처리 중 오루 -> ", e);
			throw new RuntimeException(e);
		}
		return totalSaved;
	}

	private int getTotalSize(JsonNode serviceBody) {
		return serviceBody.path(ApiResponseKeys.V_WORLD_TOTAL_COUNT.getValue()).asInt();
	}
	
	private JsonNode makeJsonFlattener(JsonNode root) {
		
		try {
			JsonNode featuresNode = root.get(ApiResponseKeys.V_WORLD_FEATURES.getValue());
			if (!featuresNode.isArray()) {
				throw new IllegalArgumentException("features 필드가 배열(Array) 아님!");
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
	        if (additionalParams.containsKey("startindex")) {
	            builder.replaceQueryParam("startindex", page);
	        }
	    }

		return openApiWebClient.get().uri(builder.build().toUri()).retrieve()
				.onStatus(status -> status.isError(), response -> {
					return response.bodyToMono(String.class).flatMap(body -> {
						log.error("API 호출 에러 발생! 응답 바디: {}", body);
						return Mono.error(new RuntimeException("API 응답 오류(" + body + ")"));
					});
				}).bodyToMono(String.class).map(res -> {
					// 응답이 XML( < 로 시작)인지 확인
					if (res.trim().startsWith("<")) {
						log.error("응답 메시지 포맷 XML : {}", res);
						throw new RuntimeException("API 서버로부터 XML 에러 메시지 수신");
					}
					try {
						return objectMapper.readTree(res); // 정상일 때만 JSON 파싱
					} catch (Exception e) {
						throw new RuntimeException("JSON 파싱 오류", e);
					}
				}).block();
	}
}
