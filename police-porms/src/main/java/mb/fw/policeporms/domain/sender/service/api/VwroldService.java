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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
import mb.fw.policeporms.common.utils.InterfaceEncryptUtils; // 📌 암호화 유틸 임포트 추가
import mb.fw.policeporms.common.utils.LoggingUtils;
import mb.fw.policeporms.common.utils.WebClientUtils;
import mb.fw.policeporms.domain.sender.service.base.AbstractApiService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@SenderComponent
public class VwroldService extends AbstractApiService {

	private static final int DEFAULT_TIMEOUT_SECONDS = 30;
	private static final int MAX_RETRY_COUNT = 3;
	private static final int MAX_CONCURRENCY = 10; 
	private static final String PARAM_START_INDEX = "startindex";
	private static final String PARAM_BBOX = "bbox";
	private static final String PARAM_USE_GRID_MODE = "useGridMode";

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

		if (additionalParams != null && additionalParams.containsKey(PARAM_USE_GRID_MODE)) {
			useGridMode = Boolean.parseBoolean(String.valueOf(additionalParams.get(PARAM_USE_GRID_MODE)));
			// additionalParams.remove(PARAM_USE_GRID_MODE); // 송신 서비스 타임아웃 처리를 위해 주석/삭제 유지
		}

		if (useGridMode) {
			log.info("[{}] 대용량 모드: QuadTree 알고리즘 + WebFlux 멀티쓰레드 병렬 처리 시작", spec.getInterfaceId());
			return fetchAndSaveWithQuadTree(spec, tempFile, transactionId);
		} else {
			log.info("[{}] 일반 페이징 모드로 수집을 시작합니다.", spec.getInterfaceId());
			return fetchAndSaveNormal(spec, tempFile, transactionId);
		}
	}

	private static class Bbox {
		final double minX, minY, maxX, maxY;
		Bbox(double minX, double minY, double maxX, double maxY) {
			this.minX = minX; this.minY = minY; this.maxX = maxX; this.maxY = maxY;
		}
		String toParam() { return String.format("%f,%f,%f,%f", minX, minY, maxX, maxY); }
	}

	private static class FetchState {
		final int page, localSaved, totalCount;
		final boolean isFinished;
		FetchState(int page, int localSaved, int totalCount, boolean isFinished) {
			this.page = page; this.localSaved = localSaved; this.totalCount = totalCount; this.isFinished = isFinished;
		}
	}

	// 대용량 병렬 처리 로직 
	private int fetchAndSaveWithQuadTree(InterfaceSpec spec, Path tempFile, String transactionId) {
		AtomicInteger grandTotalSaved = new AtomicInteger(0);
		Object fileLock = new Object(); 
		boolean isEncrypt = spec.isDataEncrypt();
		
		List<Bbox> rootBboxes = generateKoreaGridBboxes(20, 20); 
		Path progressFile = Paths.get(tempFile.toString() + ".progress");
		Set<String> completedBboxes = Collections.synchronizedSet(readProgress(progressFile));
		
		List<Bbox> bboxesToProcess = new ArrayList<>();
		for (Bbox b : rootBboxes) if (!completedBboxes.contains(b.toParam())) bboxesToProcess.add(b);

		log.info("[{}] 남은 {}개의 최상위 격자를 최대 {}개씩 병렬 수집 (암호화 적용: {})", spec.getInterfaceId(), bboxesToProcess.size(), MAX_CONCURRENCY, isEncrypt);
		if (bboxesToProcess.isEmpty()) return 0;

		try (OutputStream fos = Files.newOutputStream(tempFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				BufferedOutputStream bos = new BufferedOutputStream(fos)) {
			
			OutputStream finalOut = bos;
			if (isEncrypt) {
				finalOut = InterfaceEncryptUtils.createFileEncryptOutputStream(finalOut);
			}

			try (GZIPOutputStream gzos = new GZIPOutputStream(finalOut);
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzos, StandardCharsets.UTF_8))) {
				Flux.fromIterable(bboxesToProcess)
					.flatMap(rootBbox -> {
						return processBboxRecursively(spec, rootBbox, 0, writer, fileLock, grandTotalSaved)
							.doOnSuccess(v -> {
								synchronized (fileLock) {
									writeProgress(progressFile, rootBbox.toParam());
									completedBboxes.add(rootBbox.toParam());
								}
								log.info("✅ 기본 격자 및 하위 분할 완료 (BBOX: {})", rootBbox.toParam());
							});
					}, MAX_CONCURRENCY) 
					.blockLast(); 
				Files.deleteIfExists(progressFile);
				LoggingUtils.printWriteFileComplete(transactionId, grandTotalSaved.get());
			}
		} catch (Exception e) { 
			log.error("[{}] 수집 중단. 다음 스케줄 실행 시 중단된 격자부터 재시작", spec.getInterfaceId(), e);
			throw new RuntimeException("API 데이터 적재 중 오류 발생", e);
		}
		return grandTotalSaved.get();
	}

	private Mono<Void> processBboxRecursively(InterfaceSpec spec, Bbox bbox, int depth, 
												BufferedWriter writer, Object fileLock, AtomicInteger grandTotalSaved) {
		return fetchPageFromApiMono(spec, 1, bbox.toParam())
			.flatMap(root -> {
				if (root == null || !root.fieldNames().hasNext() || !root.has(ApiResponseKeys.V_WORLD_FEATURES.getValue())) {
					return Mono.empty();
				}

				int totalCount = getTotalSize(root);

				if (depth > 15) {
					log.warn("⚠️ 최대 분할 깊이 초과 (BBOX: {}). 비정상 밀집 지역이므로 상위 데이터 강제 획득.", bbox.toParam());
				} else if (totalCount >= 1000) {
					double midX = (bbox.minX + bbox.maxX) / 2.0;
					double midY = (bbox.minY + bbox.maxY) / 2.0;
					List<Bbox> children = List.of(
						new Bbox(bbox.minX, bbox.minY, midX, midY),
						new Bbox(midX, bbox.minY, bbox.maxX, midY),
						new Bbox(bbox.minX, midY, midX, bbox.maxY),
						new Bbox(midX, midY, bbox.maxX, bbox.maxY)
					);
					
					return Flux.fromIterable(children)
						.concatMap(child -> processBboxRecursively(spec, child, depth + 1, writer, fileLock, grandTotalSaved))
						.then();
				} 
				
				if (totalCount > 0) {
					JsonNode rowNode = makeJsonFlattener(root);
					if (rowNode != null && rowNode.isArray()) {
						List<Map<String, Object>> rows = objectMapper.convertValue(rowNode, new TypeReference<List<Map<String, Object>>>() {});
						
						synchronized (fileLock) {
							writeRowsToWriter(writer, rows);
							int currentGrandTotal = grandTotalSaved.addAndGet(rows.size());
							if (currentGrandTotal % 100000 < 2000) {
								log.info("[{}] 📈 누적 수집 건수: {} 건 (안전격자 수집)", spec.getInterfaceId(), String.format("%,d", currentGrandTotal));
							}
						}
					}
				}
				return Mono.empty();
			});
	}

	// 일반 데이터 전용 로직 
	private int fetchAndSaveNormal(InterfaceSpec spec, Path tempFile, String transactionId) {
		if (spec.getAdditionalParams() != null) spec.getAdditionalParams().remove(PARAM_BBOX);
		
		AtomicInteger grandTotalSaved = new AtomicInteger(0);
		Object fileLock = new Object(); 
		boolean isEncrypt = spec.isDataEncrypt();

		try (OutputStream fos = Files.newOutputStream(tempFile, StandardOpenOption.CREATE);
				BufferedOutputStream bos = new BufferedOutputStream(fos)) {
			
			OutputStream finalOut = bos;
			if (isEncrypt) {
				finalOut = InterfaceEncryptUtils.createFileEncryptOutputStream(finalOut);
			}

			try (GZIPOutputStream gzos = new GZIPOutputStream(finalOut);
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzos, StandardCharsets.UTF_8))) {
				fetchAndWritePageNormal(spec, 1, 0, -1, writer, fileLock, transactionId, grandTotalSaved)
					.expand(state -> {
						if (state.isFinished) return Mono.empty();
						return fetchAndWritePageNormal(spec, state.page + 1, state.localSaved, state.totalCount, writer, fileLock, transactionId, grandTotalSaved);
					})
					.blockLast(); 
				LoggingUtils.printWriteFileComplete(transactionId, grandTotalSaved.get());
			}
		} catch (Exception e) { 
			log.error("[{}] 일반 모드 오류 발생", spec.getInterfaceId(), e);
			try { Files.deleteIfExists(tempFile); } catch (IOException ex) {}
			throw new RuntimeException("API 적재 오류", e);
		}
		return grandTotalSaved.get();
	}

	private Mono<FetchState> fetchAndWritePageNormal(InterfaceSpec spec, int page, int currentLocalSaved, int currentTotalCount, 
													 BufferedWriter writer, Object fileLock, String transactionId, AtomicInteger grandTotalSaved) {
		return fetchPageFromApiMono(spec, page, null)
			.map(root -> {
				if (root == null || !root.fieldNames().hasNext() || !root.has(ApiResponseKeys.V_WORLD_FEATURES.getValue())) {
					return new FetchState(page, currentLocalSaved, currentTotalCount, true);
				}
				
				int totalCount = currentTotalCount == -1 ? getTotalSize(root) : currentTotalCount;
				JsonNode rowNode = makeJsonFlattener(root);
				
				if (rowNode != null && rowNode.isArray()) {
					List<Map<String, Object>> rows = objectMapper.convertValue(rowNode, new TypeReference<List<Map<String, Object>>>() {});
					
					int newLocalSaved = currentLocalSaved;
					if (totalCount > 0 && (newLocalSaved + rows.size()) > totalCount) {
						int allowedSize = totalCount - newLocalSaved;
						if (allowedSize > 0) rows = rows.subList(0, allowedSize);
						else rows.clear();
					}

					if (!rows.isEmpty()) {
						synchronized (fileLock) {
							writeRowsToWriter(writer, rows);
							newLocalSaved += rows.size();
							grandTotalSaved.addAndGet(rows.size());
							if (rows.size() < spec.getApiRequestFetchSize() && newLocalSaved != totalCount) totalCount = newLocalSaved;
							LoggingUtils.printWriteFileProgress(transactionId, rows.size(), newLocalSaved, totalCount);
						}
					}
					
					boolean isFinished = rows.size() < spec.getApiRequestFetchSize() || newLocalSaved >= totalCount || !spec.isLoopCall();
					return new FetchState(page, newLocalSaved, totalCount, isFinished);
				}
				return new FetchState(page, currentLocalSaved, totalCount, true);
			});
	}


	private Mono<JsonNode> fetchPageFromApiMono(InterfaceSpec spec, int page, String bbox) {
		String apiPath = String.format("/%s", spec.getApiServiceId());
		UriComponentsBuilder builder = WebClientUtils.appendQueryParams(spec, apiPath);
		
		Map<String, Object> additionalParams = spec.getAdditionalParams();
		if (additionalParams != null && additionalParams.containsKey(PARAM_START_INDEX)) {
			builder.replaceQueryParam(PARAM_START_INDEX, page);
		}
		if (bbox != null) builder.replaceQueryParam(PARAM_BBOX, bbox);

		int timeoutSeconds = spec.getApiRequestTimeoutSeconds() > 0 ? spec.getApiRequestTimeoutSeconds() : DEFAULT_TIMEOUT_SECONDS;

		return openApiWebClient.get().uri(builder.build().toUri()).retrieve()
				.onStatus(status -> status.isError(), response -> response.bodyToMono(String.class).flatMap(body -> Mono.error(new RuntimeException("API 응답 오류(" + body + ")"))))
				.bodyToMono(String.class)
				.map(res -> {
					if (res.trim().startsWith("<")) throw new RuntimeException("API 서버로부터 XML 에러 수신");
					try { return objectMapper.readTree(res); } catch (Exception e) { throw new RuntimeException("JSON 파싱 오류", e); }
				})
				.retryWhen(Retry.fixedDelay(MAX_RETRY_COUNT, Duration.ofSeconds(3))
					.doBeforeRetry(rs -> log.warn("⚠️ [{}] API 호출 실패 (재시도 {}/{}). 원인: {}", spec.getInterfaceId(), rs.totalRetriesInARow() + 1, MAX_RETRY_COUNT, rs.failure().getMessage()))
					.onRetryExhaustedThrow((spec_, rs) -> new RuntimeException("최대 재시도 횟수 초과", rs.failure()))
				);
	}

	private List<Bbox> generateKoreaGridBboxes(int cols, int rows) {
		List<Bbox> bboxes = new ArrayList<>();
		double minX = 600000.0, minY = 1300000.0;
		double maxX = 1500000.0, maxY = 2200000.0;
		double stepX = (maxX - minX) / cols, stepY = (maxY - minY) / rows;
		for (int i = 0; i < cols; i++) {
			for (int j = 0; j < rows; j++) {
				bboxes.add(new Bbox(minX + (stepX * i), minY + (stepY * j), (minX + (stepX * i)) + stepX, (minY + (stepY * j)) + stepY));
			}
		}
		return bboxes;
	}

	private Set<String> readProgress(Path progressFile) {
		Set<String> completed = new HashSet<>();
		if (Files.exists(progressFile)) {
			try { completed.addAll(Files.readAllLines(progressFile, StandardCharsets.UTF_8)); } 
			catch (IOException e) { log.warn("Progress 읽기 실패: {}", e.getMessage()); }
		}
		return completed;
	}

	private void writeProgress(Path progressFile, String bbox) {
		try { Files.write(progressFile, (bbox + System.lineSeparator()).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND); } 
		catch (IOException e) { log.error("Progress 쓰기 실패: {}", e.getMessage()); }
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
}