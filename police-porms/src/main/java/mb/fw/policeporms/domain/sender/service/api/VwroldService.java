package mb.fw.policeporms.domain.sender.service.api;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

	// 📌 하드코딩 방지를 위한 상수 정의
	private static final int DEFAULT_TIMEOUT_SECONDS = 30;
	private static final int MAX_RETRY_COUNT = 3;
	private static final String PARAM_START_INDEX = "startindex";
	private static final String PARAM_BBOX = "bbox";
	private static final String PARAM_USE_GRID_MODE = "useGridMode";
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
		boolean useGridMode = false;
		Map<String, Object> additionalParams = spec.getAdditionalParams();

		// 1. JSON 설정에서 대용량 모드(Grid) 활성화 여부 확인 및 파라미터 안전 제거
		if (additionalParams != null && additionalParams.containsKey(PARAM_USE_GRID_MODE)) {
			useGridMode = Boolean.parseBoolean(String.valueOf(additionalParams.get(PARAM_USE_GRID_MODE)));
			additionalParams.remove(PARAM_USE_GRID_MODE);
		}

		// 2. 플래그 값에 따라 분기 처리
		if (useGridMode) {
			log.info("[{}] JSON 설정 감지: 대용량 데이터 모드(Grid BBOX)로 수집을 시작합니다.", spec.getInterfaceId());
			return fetchAndSaveWithGrid(spec, tempFile, transactionId);
		} else {
			log.info("[{}] 일반 페이징 모드로 수집을 시작합니다.", spec.getInterfaceId());
			return fetchAndSaveNormal(spec, tempFile, transactionId);
		}
	}

	// =========================================================================
	// 📍 [모드 1] 대용량 데이터 전용 로직 (BBOX 격자 분할 + 재시작 + 누적 진행률)
	// =========================================================================
	private int fetchAndSaveWithGrid(InterfaceSpec spec, Path tempFile, String transactionId) {
		int grandTotalSaved = 0;
		int fetchSize = spec.getApiRequestFetchSize();
		
		// 10x10 격자로 전국 분할
		List<String> bboxes = generateKoreaGridBboxes(10, 10);
		
		Path progressFile = Paths.get(tempFile.toString() + ".progress");
		Set<String> completedBboxes = readProgress(progressFile);
		
		log.info("[{}] 전체 {}개의 격자 중 {}개 완료 상태. 이어서 수집을 시작합니다.", spec.getInterfaceId(), bboxes.size(), completedBboxes.size());

		try {
			for (int i = 0; i < bboxes.size(); i++) {
				String bbox = bboxes.get(i);
				
				// 이미 성공한 격자 스킵 (Resume)
				if (completedBboxes.contains(bbox)) {
					log.debug("⏩ [{}/{}] 격자({})는 이미 수집 완료되어 건너뜁니다.", i + 1, bboxes.size(), bbox);
					continue;
				}

				log.info("🌐 [{}/{}] 격자 수집 시작 (BBOX: {})", i + 1, bboxes.size(), bbox);
				spec.getAdditionalParams().put(PARAM_BBOX, bbox);
				
				int page = 1;
				int totalCount = -1;
				int localSaved = 0;

				// APPEND 모드로 이어쓰기
				try (OutputStream fos = Files.newOutputStream(tempFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
						BufferedOutputStream bos = new BufferedOutputStream(fos);
						GZIPOutputStream gzos = new GZIPOutputStream(bos);
						BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzos, StandardCharsets.UTF_8))) {
					
					while (true) {
						JsonNode root = fetchPageFromApiWithRetry(spec, page);
						
						if (root == null || !root.fieldNames().hasNext()) break;
						
						if(root.has(ApiResponseKeys.V_WORLD_RESULT_EXCEPTIONS.getValue())) {
							JsonNode exceptionsNode = root.path(ApiResponseKeys.V_WORLD_RESULT_EXCEPTIONS.getValue());
							if (exceptionsNode.isArray() && exceptionsNode.size() > 0) {
								String resultExpCode = exceptionsNode.get(0).path(RESPONSE_ERROR_CODE).asText();
								String resultExptext = exceptionsNode.get(0).path(RESPONSE_ERROR_TEXT).asText();
								log.error("[{}] API error code: {}, text: {}, at page {}", spec.getInterfaceId(), resultExpCode, resultExptext, page);
							}
							break;
						}
						
						if (totalCount == -1) totalCount = getTotalSize(root);
						if (!root.has(ApiResponseKeys.V_WORLD_FEATURES.getValue())) break;
						
						JsonNode rowNode = makeJsonFlattener(root);
						
						if (rowNode != null && rowNode.isArray()) {
							List<Map<String, Object>> rows = objectMapper.convertValue(rowNode, new TypeReference<List<Map<String, Object>>>() {});

							// 초과 데이터 절삭
							if (totalCount > 0 && (localSaved + rows.size()) > totalCount) {
								int allowedSize = totalCount - localSaved;
								if (allowedSize > 0) rows = rows.subList(0, allowedSize);
								else rows.clear();
							}

							if (!rows.isEmpty()) {
								writeRowsToWriter(writer, rows);
								localSaved += rows.size();
								grandTotalSaved += rows.size();
								
								if (rows.size() < fetchSize && localSaved != totalCount) {
									totalCount = localSaved;
								}
								
								// 개별 격자 진행률 및 전체 누적 진행률 출력
								LoggingUtils.printWriteFileProgress(transactionId + "-GRID-" + (i+1), rows.size(), localSaved, totalCount);
								log.info("[{}] 📈 현재까지 총 누적 수집 건수: {} 건", spec.getInterfaceId(), String.format("%,d", grandTotalSaved));
							}
							if (rows.size() < fetchSize || localSaved >= totalCount) break;
						} else {
							break;
						}
						page++;
						if (!spec.isLoopCall()) break;
					}
				}
				// 하나의 격자가 무사히 끝나면 progress 파일에 기록
				writeProgress(progressFile, bbox);
				log.info("✅ [{}/{}] 격자 완료 (해당 격자 수집 건수: {}, 누적 총 건수: {})", i + 1, bboxes.size(), String.format("%,d", localSaved), String.format("%,d", grandTotalSaved));
			}

			// 배치가 정상 종료되면 progress 파일 삭제
			Files.deleteIfExists(progressFile);
			LoggingUtils.printWriteFileComplete(transactionId, grandTotalSaved);
			
		} catch (Exception e) { 
			// Grid 모드는 중단된 곳부터 이어서 해야 하므로 임시 파일을 삭제하지 않음
			log.error("[{}] 대용량 데이터 처리 중 오류 발생. 다음 스케줄 실행 시 중단된 격자부터 재시작됩니다.", spec.getInterfaceId(), e);
			throw new RuntimeException("API 데이터 적재 중 오류 발생", e);
		}
		return grandTotalSaved;
	}

	// =========================================================================
	// 📍 [모드 2] 일반 데이터 전용 로직 (단순 페이징)
	// =========================================================================
	private int fetchAndSaveNormal(InterfaceSpec spec, Path tempFile, String transactionId) {
		int totalSaved = 0;
		int page = 1;
		int fetchSize = spec.getApiRequestFetchSize();
		int totalCount = -1;
		
		// 혹시 캐싱된 파라미터에 남아있을지 모르는 bbox 제거
		if (spec.getAdditionalParams() != null) {
			spec.getAdditionalParams().remove(PARAM_BBOX);
		}
		
		// CREATE 모드로 새 파일 생성
		try (OutputStream fos = Files.newOutputStream(tempFile, StandardOpenOption.CREATE);
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				GZIPOutputStream gzos = new GZIPOutputStream(bos);
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzos, StandardCharsets.UTF_8))) {
			
			while (true) {
				JsonNode root = fetchPageFromApiWithRetry(spec, page);
				
				if (root == null || !root.fieldNames().hasNext()) {
					log.warn("[{}] Empty response from API at page {}", spec.getInterfaceId(), page);
					break;
				}
				
				if(root.has(ApiResponseKeys.V_WORLD_RESULT_EXCEPTIONS.getValue())) {
					JsonNode exceptionsNode = root.path(ApiResponseKeys.V_WORLD_RESULT_EXCEPTIONS.getValue());
					if (exceptionsNode.isArray() && exceptionsNode.size() > 0) {
						JsonNode firstException = exceptionsNode.get(0);
						String resultExpCode = firstException.path(RESPONSE_ERROR_CODE).asText();
						String resultExptext = firstException.path(RESPONSE_ERROR_TEXT).asText();
						log.error("[{}] API error code: {}, text: {}, at page {}", spec.getInterfaceId(), resultExpCode, resultExptext, page);
					}
					break;
				}
				
				if (totalCount == -1) totalCount = getTotalSize(root);

				if(!root.has(ApiResponseKeys.V_WORLD_FEATURES.getValue())) break;
				
				JsonNode rowNode = makeJsonFlattener(root);
				
				if (rowNode != null && rowNode.isArray()) {
					List<Map<String, Object>> rows = objectMapper.convertValue(rowNode, new TypeReference<List<Map<String, Object>>>() {});

					if (totalCount > 0 && (totalSaved + rows.size()) > totalCount) {
						int allowedSize = totalCount - totalSaved;
						if (allowedSize > 0) rows = rows.subList(0, allowedSize);
						else rows.clear();
					}

					if (!rows.isEmpty()) {
						writeRowsToWriter(writer, rows);
						totalSaved += rows.size();
						
						if (rows.size() < fetchSize && totalSaved != totalCount) {
							totalCount = totalSaved;
						}
						LoggingUtils.printWriteFileProgress(transactionId, rows.size(), totalSaved, totalCount);
					}

					if (rows.size() < fetchSize || totalSaved >= totalCount) break;
				} else {
					break;
				}
				page++;
				if (!spec.isLoopCall()) break;
			}
			
			LoggingUtils.printWriteFileComplete(transactionId, totalSaved);
			
		} catch (Exception e) { 
			// 일반 모드는 예외 발생 시 잘못 만들어진 파일을 바로 삭제
			log.error("[{}] 일반 모드 처리 중 오류 발생, 생성 중인 파일 삭제 시도 -> ", spec.getInterfaceId(), e);
			try {
				Files.deleteIfExists(tempFile);
			} catch (IOException ex) {
				log.error("[{}] 임시 파일 삭제 실패: {}", spec.getInterfaceId(), tempFile, ex);
			}
			throw new RuntimeException("API 데이터 적재 중 오류 발생", e);
		}
		return totalSaved;
	}

	// =========================================================================
	// 🛠️ 공통 유틸리티 메서드 
	// =========================================================================

	private JsonNode fetchPageFromApiWithRetry(InterfaceSpec spec, int page) {
		int attempt = 0;
		while (attempt < MAX_RETRY_COUNT) {
			try {
				return fetchPageFromApi(spec, page);
			} catch (Exception e) {
				attempt++;
				log.warn("⚠️ [{}] API 호출 실패 (재시도 {}/{}). 원인: {}", spec.getInterfaceId(), attempt, MAX_RETRY_COUNT, e.getMessage());
				if (attempt >= MAX_RETRY_COUNT) {
					log.error("❌ [{}] 최대 재시도 횟수 초과. 배치를 중단합니다.", spec.getInterfaceId());
					throw e;
				}
				try { Thread.sleep(3000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
			}
		}
		return null;
	}

	private List<String> generateKoreaGridBboxes(int cols, int rows) {
		List<String> bboxes = new ArrayList<>();
		double minX = 700000.0, minY = 1300000.0;
		double maxX = 1400000.0, maxY = 2200000.0;
		
		double stepX = (maxX - minX) / cols;
		double stepY = (maxY - minY) / rows;
		
		for (int i = 0; i < cols; i++) {
			for (int j = 0; j < rows; j++) {
				double curMinX = minX + (stepX * i);
				double curMinY = minY + (stepY * j);
				double curMaxX = curMinX + stepX;
				double curMaxY = curMinY + stepY;
				bboxes.add(String.format("%f,%f,%f,%f", curMinX, curMinY, curMaxX, curMaxY));
			}
		}
		return bboxes;
	}

	private Set<String> readProgress(Path progressFile) {
		Set<String> completed = new HashSet<>();
		if (Files.exists(progressFile)) {
			try {
				completed.addAll(Files.readAllLines(progressFile, StandardCharsets.UTF_8));
			} catch (IOException e) {
				log.warn("Progress 파일 읽기 실패: {}", e.getMessage());
			}
		}
		return completed;
	}

	private void writeProgress(Path progressFile, String bbox) {
		try {
			Files.write(progressFile, (bbox + System.lineSeparator()).getBytes(StandardCharsets.UTF_8), 
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			log.error("Progress 파일 쓰기 실패: {}", e.getMessage());
		}
	}

	private int getTotalSize(JsonNode serviceBody) {
		return serviceBody.path(ApiResponseKeys.V_WORLD_TOTAL_COUNT.getValue()).asInt();
	}
	
	private JsonNode makeJsonFlattener(JsonNode root) {
		try {
			JsonNode featuresNode = root.get(ApiResponseKeys.V_WORLD_FEATURES.getValue());
			if (featuresNode == null || !featuresNode.isArray()) return new ObjectMapper().createArrayNode();
			
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
						if (value != null && !value.isNull()) flattenedFeature.put(key, value.toString());
						else flattenedFeature.set(key, value);
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
			if (additionalParams.containsKey(PARAM_START_INDEX)) builder.replaceQueryParam(PARAM_START_INDEX, page);
			if (additionalParams.containsKey(PARAM_BBOX)) builder.replaceQueryParam(PARAM_BBOX, additionalParams.get(PARAM_BBOX));
		}

		int timeoutSeconds = spec.getApiRequestTimeoutSeconds() > 0 ? spec.getApiRequestTimeoutSeconds() : DEFAULT_TIMEOUT_SECONDS;

		return openApiWebClient.get().uri(builder.build().toUri()).retrieve()
				.onStatus(status -> status.isError(), response -> {
					return response.bodyToMono(String.class).flatMap(body -> Mono.error(new RuntimeException("API 응답 오류(" + body + ")")));
				}).bodyToMono(String.class).map(res -> {
					if (res.trim().startsWith("<")) throw new RuntimeException("API 서버로부터 XML 에러 메시지 수신");
					try {
						return objectMapper.readTree(res);
					} catch (Exception e) {
						throw new RuntimeException("JSON 파싱 오류", e);
					}
				}).block(Duration.ofSeconds(timeoutSeconds)); 
	}
}