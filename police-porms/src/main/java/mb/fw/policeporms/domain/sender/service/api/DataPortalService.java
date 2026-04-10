package mb.fw.policeporms.domain.sender.service.api;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import lombok.extern.slf4j.Slf4j;
import mb.fw.policeporms.common.annotation.SenderComponent;
import mb.fw.policeporms.common.constant.ApiResponseKeys;
import mb.fw.policeporms.common.constant.ApiType;
import mb.fw.policeporms.common.spec.InterfaceSpec;
import mb.fw.policeporms.common.utils.ESBProductEncryption;
import mb.fw.policeporms.common.utils.LoggingUtils;
import mb.fw.policeporms.common.utils.WebClientUtils;
import mb.fw.policeporms.domain.sender.service.base.AbstractApiService;
import reactor.core.publisher.Mono;

@Slf4j
@SenderComponent
public class DataPortalService extends AbstractApiService {

	private static final String PARAM_SERVICE_KEY = "serviceKey";
	private static final String PARAM_DETAIL_SERVICE_ID = "detailServiceId";
	private static final String PARAM_DETAIL_KEY = "detailKey";
	private static final String PARAM_DETAIL_ONLY_PARAMS = "detailOnlyParams";
	private static final String PARAM_MASTER_ONLY_PARAMS = "masterOnlyParams"; 
	private static final String PREFIX_CURRENT_DATE = "CURRENT_DATE";
	private static final String DUMMY_SINGLE_CALL = "SINGLE_CALL_DUMMY";

	protected DataPortalService(ObjectMapper objectMapper, WebClient openApiWebClient) {
		super(objectMapper, openApiWebClient);
	}

	@Override
	public ApiType getApiType() {
		return ApiType.DATA_PORTAL;
	}

	@Override
	public int fetchAndSave(InterfaceSpec spec, Path tempFile, String transactionId) {
		int totalSaved = 0;
		int fetchSize = spec.getApiRequestFetchSize();

		Map<String, Object> additionalParams = spec.getAdditionalParams() != null 
				? new HashMap<>(spec.getAdditionalParams()) 
				: new HashMap<>();

		// serviceKey 암호화(ENC) 복호화 처리
		String[] keyCandidates = {"serviceKey", "ServiceKey"};
		for (String keyName : keyCandidates) {
			if (additionalParams.containsKey(keyName)) {
				String encryptedKey = String.valueOf(additionalParams.get(keyName));
				if (encryptedKey.startsWith("ENC(") && encryptedKey.endsWith(")")) {
					try {
						String decryptedKey = ESBProductEncryption.decryptString(encryptedKey);
						additionalParams.put(keyName, decryptedKey);
					} catch (Exception e) {
						log.error("[{}] serviceKey 복호화 오류", spec.getInterfaceId(), e);
						throw new RuntimeException("API serviceKey 복호화 실패", e); 
					}
				}
			}
		}

		// 동적 날짜 자동 치환 및 계산
		LocalDateTime now = LocalDateTime.now();
		Map<String, Object> dateUpdates = new HashMap<>();
		for (Map.Entry<String, Object> entry : additionalParams.entrySet()) {
			Object valueObj = entry.getValue();
			if (valueObj instanceof String) {
				String valStr = ((String) valueObj).trim();
				
				if (valStr.startsWith(PREFIX_CURRENT_DATE)) {
					int colonIndex = valStr.indexOf(":");
					if (colonIndex != -1) {
						String mathPart = valStr.substring(PREFIX_CURRENT_DATE.length(), colonIndex).trim();
						String formatPattern = valStr.substring(colonIndex + 1).trim();
						
						LocalDateTime targetDate = now;
						
						if (!mathPart.isEmpty()) {
							try {
								int daysOffset = Integer.parseInt(mathPart);
								targetDate = targetDate.plusDays(daysOffset);
							} catch (NumberFormatException e) {
								log.error("[{}] 날짜 계산식 파싱 오류 (무시됨): {}", spec.getInterfaceId(), mathPart, e);
							}
						}
						
						try {
							String formattedDate = targetDate.format(DateTimeFormatter.ofPattern(formatPattern));
							dateUpdates.put(entry.getKey(), formattedDate);
						} catch (IllegalArgumentException e) {
							log.error("[{}] 잘못된 날짜 포맷: {}", spec.getInterfaceId(), formatPattern, e);
						}
					}
				}
			}
		}
		if (!dateUpdates.isEmpty()) additionalParams.putAll(dateUpdates);

		// 외부 파일(txt, csv)을 읽어서 동적 배열(단일/다중)로 변환
		Map<String, Object> fileUpdates = new HashMap<>();
		List<String> keysToRemove = new ArrayList<>();

		for (Map.Entry<String, Object> entry : additionalParams.entrySet()) {
			if (entry.getValue() instanceof String) {
				String valStr = ((String) entry.getValue()).trim();
				if (valStr.startsWith("FILE:")) {
					String filePath = valStr.substring(5).trim();
					String paramKey = entry.getKey(); 
					
					try {
						List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
						List<Object> parsedList = new ArrayList<>();
						
						if (paramKey.contains(",")) {
							String[] keys = paramKey.split("\\s*,\\s*");
							for (String line : lines) {
								if (line.trim().isEmpty()) continue; 
								String[] values = line.split("\\s*,\\s*");
								
								Map<String, String> map = new HashMap<>();
								for (int i = 0; i < keys.length && i < values.length; i++) {
									map.put(keys[i], values[i]); 
								}
								parsedList.add(map);
							}
							fileUpdates.put("MULTI_FILE_PARAM_LIST", parsedList);
							keysToRemove.add(paramKey);
						} else {
							for (String line : lines) {
								if (!line.trim().isEmpty()) parsedList.add(line.trim());
							}
							fileUpdates.put(paramKey, parsedList);
						}
						log.info("[{}] 외부 파라미터 파일 로드 완료: {} (총 {}건)", spec.getInterfaceId(), filePath, parsedList.size());
					} catch (IOException e) {
						log.error("[{}] 외부 파라미터 파일 읽기 실패: {}", spec.getInterfaceId(), filePath, e);
						throw new RuntimeException("외부 파라미터 파일 읽기 실패", e);
					}
				}
			}
		}
		
		for (String k : keysToRemove) additionalParams.remove(k);
		if (!fileUpdates.isEmpty()) additionalParams.putAll(fileUpdates);

		// 리스트(배열) 파라미터 추출
		String loopParamKey = null;
		List<?> loopParamValues = null;

		for (Map.Entry<String, Object> entry : additionalParams.entrySet()) {
			if (entry.getValue() instanceof List) {
				loopParamKey = entry.getKey();
				loopParamValues = (List<?>) entry.getValue();
				break; 
			}
		}
		
		if (loopParamKey != null) {
			additionalParams.remove(loopParamKey);
		}

		if (loopParamValues == null || loopParamValues.isEmpty()) {
			loopParamValues = Arrays.asList(DUMMY_SINGLE_CALL);
		}

		// 상세 API 관련 파라미터 추출
		boolean useDetailApi = additionalParams.containsKey(PARAM_DETAIL_SERVICE_ID);
		String[] detailKeys = new String[0]; 
		if (useDetailApi && additionalParams.containsKey(PARAM_DETAIL_KEY)) {
			Object detailKeyObj = additionalParams.get(PARAM_DETAIL_KEY);
			if (detailKeyObj != null && !String.valueOf(detailKeyObj).trim().isEmpty()) {
				detailKeys = String.valueOf(detailKeyObj).split("\\s*,\\s*");
			}
		}

		try (OutputStream fos = Files.newOutputStream(tempFile, StandardOpenOption.CREATE);
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				GZIPOutputStream gzos = new GZIPOutputStream(bos); 
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzos, StandardCharsets.UTF_8))) {
			
			int totalArraySize = loopParamValues.size();
			int currentArrayIndex = 0;

			for (Object loopValue : loopParamValues) {
				currentArrayIndex++; 
				List<String> injectedKeys = new ArrayList<>();

				if (loopValue instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> mapValue = (Map<String, Object>) loopValue;
					additionalParams.putAll(mapValue);         
					injectedKeys.addAll(mapValue.keySet());    
				} else if (loopParamKey != null && !DUMMY_SINGLE_CALL.equals(loopValue)) {
					additionalParams.put(loopParamKey, loopValue); 
					injectedKeys.add(loopParamKey);                
				}

				int page = 1;
				int currentLoopTotalCount = -1; 
				int currentLoopSaved = 0;       
				int currentMasterSaved = 0; // 프로그레스 및 페이징 종료를 위한 순수 마스터 누적 건수

				while (true) {
					// 마스터 API 호출
					JsonNode root = fetchPageFromApi(spec, page, additionalParams);
					if (root == null || root.isEmpty()) break;
					
					JsonNode headerNode = root.findPath(ApiResponseKeys.DATA_PORTAL_HEADER.getValue());
					String resultCode = headerNode.path(ApiResponseKeys.DATA_PORTAL_RESULT_CODE.getValue()).asText().trim();
					
					if (!ApiResponseKeys.DATA_PORTAL_RESULT_SUCCESS.getValue().equals(resultCode) && !"0000".equals(resultCode)) {
						log.error("[{}] API error code: {} at page {}", spec.getInterfaceId(), resultCode, page);
						break;
					}
					
					if (currentLoopTotalCount == -1) {
						currentLoopTotalCount = getTotalSize(root);
					}

					boolean isMultiCall = !DUMMY_SINGLE_CALL.equals(loopValue);

					if (currentLoopTotalCount == 0) {
						if (isMultiCall) {
							log.warn("[{}] 배열 [{}/{}] 0건 리턴 (파라미터: {}) -> 스킵 (현재 총 누적: {}건)", 
									spec.getInterfaceId(), currentArrayIndex, totalArraySize, loopValue, totalSaved);
						} else {
							log.warn("[{}] 데이터 0건 리턴 -> 단일 호출 조기 종료", spec.getInterfaceId());
						}
						break;
					}

					JsonNode rowNode = root.findPath(ApiResponseKeys.DATA_PORTAL_ITEMS_DATA.getValue());
					
					if (rowNode.isMissingNode() || rowNode.isNull() || (rowNode.isTextual() && rowNode.asText().trim().isEmpty())) {
						if (isMultiCall) {
							log.warn("[{}] 배열 [{}/{}] items 빈 태그 (파라미터: {}) -> 스킵 (현재 총 누적: {}건)", 
									spec.getInterfaceId(), currentArrayIndex, totalArraySize, loopValue, totalSaved);
						} else {
							log.warn("[{}] items 태그가 비어있습니다 -> 단일 호출 조기 종료", spec.getInterfaceId());
						}
						break;
					}
					
					if (rowNode.isObject() && rowNode.has(ApiResponseKeys.DATA_PORTAL_ITEMS_WEA_DATA.getValue())) {
						rowNode = rowNode.get(ApiResponseKeys.DATA_PORTAL_ITEMS_WEA_DATA.getValue());
					}
					
					if (rowNode != null) {
						List<Map<String, Object>> rows = new ArrayList<>();
						
						if (rowNode.isArray()) {
							rows = objectMapper.convertValue(rowNode, new TypeReference<List<Map<String, Object>>>() {});
						} else if (rowNode.isObject()) {
							Map<String, Object> singleRow = objectMapper.convertValue(rowNode, new TypeReference<Map<String, Object>>() {});
							rows.add(singleRow);
						}

						if (!rows.isEmpty()) {
							int originalMasterSize = rows.size();
							
							int detailSuccessCount = 0; 
							int detailTotalFetchedCount = 0; 
							
							if (useDetailApi && detailKeys.length > 0) {
								String detailServiceId = String.valueOf(additionalParams.get(PARAM_DETAIL_SERVICE_ID));
								
								int totalDetailSize = rows.size();
								int currentDetailIdx = 0;
								
								List<Map<String, Object>> expandedRows = new ArrayList<>();
								
								for (Map<String, Object> row : rows) {
									currentDetailIdx++;
									Map<String, String> extractedKeys = new HashMap<>();
									boolean hasAllKeys = true;
									
									for (String keyDef : detailKeys) {
										String masterKey = keyDef;
										String detailParamName = keyDef;
										
										if (keyDef.contains(":")) {
											String[] parts = keyDef.split(":");
											if (parts.length >= 2) {
												masterKey = parts[0].trim();
												detailParamName = parts[1].trim();
											}
										}
										
										Object valObj = row.get(masterKey);
										if (valObj != null && !String.valueOf(valObj).trim().isEmpty()) {
											extractedKeys.put(detailParamName, String.valueOf(valObj));
										} else {
											hasAllKeys = false;
											break; 
										}
									}
									
									int currentFetchCount = 0; 
									
									if (hasAllKeys && !extractedKeys.isEmpty()) {
										try {
											JsonNode detailNode = fetchDetailFromApi(spec, detailServiceId, extractedKeys, additionalParams);
											JsonNode detailDataNode = extractDataNode(detailNode);
											
											if (detailDataNode != null) {
												if (detailDataNode.isArray()) {
													currentFetchCount = detailDataNode.size();
													if (currentFetchCount > 0) {
														for (JsonNode itemNode : detailDataNode) {
															Map<String, Object> newExpandedRow = new HashMap<>(row); 
															Map<String, Object> detailMap = objectMapper.convertValue(itemNode, new TypeReference<Map<String, Object>>() {});
															newExpandedRow.putAll(detailMap); 
															expandedRows.add(newExpandedRow); 
														}
													} else {
														expandedRows.add(row); 
													}
												} else if (detailDataNode.isObject()) {
													currentFetchCount = 1;
													Map<String, Object> newExpandedRow = new HashMap<>(row);
													Map<String, Object> detailMap = objectMapper.convertValue(detailDataNode, new TypeReference<Map<String, Object>>() {});
													newExpandedRow.putAll(detailMap);
													expandedRows.add(newExpandedRow);
												}
											} else {
												expandedRows.add(row); 
												log.debug("[{}] 상세 API 데이터 0건 (파라미터: {}) -> 마스터 원본 유지", spec.getInterfaceId(), extractedKeys);
											}
											
											detailSuccessCount += currentFetchCount; 
											
										} catch (Exception e) {
											log.error("[{}] 상세 API 호출 오류 (파라미터: {}) -> 마스터 원본 유지", spec.getInterfaceId(), extractedKeys, e);
											expandedRows.add(row); 
										}
									} else {
										log.warn("[{}] 마스터 데이터에 상세 조회를 위한 필수 키가 없음. 설정된 detailKey: {}", 
												spec.getInterfaceId(), Arrays.toString(detailKeys));
										expandedRows.add(row);
									}
									
									detailTotalFetchedCount += currentFetchCount; 
									
									log.info("[{}] 🔄 상세호출 [{}/{}] 마스터전체: {}건 | 현재상세 응답: {}건 | 누적상세 응답: {}건 (요청: {})", 
											spec.getInterfaceId(), 
											currentDetailIdx, totalDetailSize, 
											totalDetailSize, 
											currentFetchCount, 
											detailTotalFetchedCount, 
											extractedKeys);
								}
								
								rows = expandedRows; 
							}

							writeRowsToWriter(writer, rows);
							totalSaved += rows.size();
							currentLoopSaved += rows.size(); 
							
							// 마스터 진행 건수를 누적
							currentMasterSaved += originalMasterSize; 
							
							if (isMultiCall) {
								if (useDetailApi) {
									log.info("[{}] 배열 [{}/{}] 처리 중 (파라미터: {}) -> 마스터: {}건 (상세 1:N 확장병합: {}건) / 총 누적 적재: {}건", 
											spec.getInterfaceId(), currentArrayIndex, totalArraySize, loopValue, rows.size(), detailSuccessCount, totalSaved);
								} else {
									log.info("[{}] 배열 [{}/{}] 처리 중 (파라미터: {}) -> 이번 적재: {}건 / 총 누적 적재: {}건", 
											spec.getInterfaceId(), currentArrayIndex, totalArraySize, loopValue, rows.size(), totalSaved);
								}
							} else {
								if (useDetailApi) {
									log.info("[{}] 처리 중 -> 이번 적재: {}건 (상세 1:N 확장병합: {}건) / 총 누적 적재: {}건", 
											spec.getInterfaceId(), rows.size(), detailSuccessCount, totalSaved);
								} else {
									log.info("[{}] 처리 중 -> 이번 적재: {}건 / 총 누적 적재: {}건", 
											spec.getInterfaceId(), rows.size(), totalSaved);
								}
							}

							LoggingUtils.printWriteFileProgress(transactionId, rows.size(), currentMasterSaved, currentLoopTotalCount);
							
							// 무한 루프나 조기 종료 방지를 위해 페이징 탈출 조건 마스터 기준으로 판단
							if (originalMasterSize < fetchSize || currentMasterSaved >= currentLoopTotalCount) {
								break;
							}
							
						} else {
							break; 
						}
					} else {
						break; 
					}
					page++;

					if (!spec.isLoopCall()) break;
				} 

				for (String key : injectedKeys) {
					additionalParams.remove(key);
				}
				
			} 

		} catch (IOException e) {
			log.error("파일 처리 중 오류 -> ", e);
			throw new RuntimeException(e);
		}
		return totalSaved;
	}

	private int getTotalSize(JsonNode root) {
		JsonNode tcNode = root.findPath(ApiResponseKeys.DATA_PORTAL_TOTAL_COUNT.getValue());
		if (!tcNode.isMissingNode() && !tcNode.isNull()) {
			try {
				return Integer.parseInt(tcNode.asText().trim());
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		return 0;
	}
	
	private JsonNode extractDataNode(JsonNode root) {
		if (root == null || root.isEmpty()) return null;
		
		JsonNode totalCountNode = root.findPath(ApiResponseKeys.DATA_PORTAL_TOTAL_COUNT.getValue());
		if (!totalCountNode.isMissingNode() && "0".equals(totalCountNode.asText().trim())) {
			return null;
		}
		
		JsonNode rowNode = root.findPath(ApiResponseKeys.DATA_PORTAL_ITEMS_DATA.getValue());
		
		if (rowNode.isMissingNode() || rowNode.isNull() || (rowNode.isTextual() && rowNode.asText().trim().isEmpty())) {
			return null;
		}

		if (rowNode.isObject() && rowNode.has(ApiResponseKeys.DATA_PORTAL_ITEMS_WEA_DATA.getValue())) {
			rowNode = rowNode.get(ApiResponseKeys.DATA_PORTAL_ITEMS_WEA_DATA.getValue());
		}
		
		return rowNode;
	}

	// 마스터 API 호출
	private JsonNode fetchPageFromApi(InterfaceSpec spec, int page, Map<String, Object> processedParams) {
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
			
			builder.replaceQueryParam(PARAM_DETAIL_SERVICE_ID);
			builder.replaceQueryParam(PARAM_DETAIL_KEY);
			builder.replaceQueryParam(PARAM_MASTER_ONLY_PARAMS);
			
			if (processedParams.containsKey(PARAM_DETAIL_ONLY_PARAMS)) {
				String[] detailOnlyKeys = String.valueOf(processedParams.get(PARAM_DETAIL_ONLY_PARAMS)).split(",");
				for (String key : detailOnlyKeys) {
					builder.replaceQueryParam(key.trim());
				}
				builder.replaceQueryParam(PARAM_DETAIL_ONLY_PARAMS);
			}

			if (processedParams.containsKey("pageNo")) {
				builder.replaceQueryParam("pageNo", page);
			}
		}

		java.net.URI finalUri = builder.build(false).encode(StandardCharsets.UTF_8).toUri();
		log.debug("마스터 요청 API URL: {}", finalUri.toString());

		return openApiWebClient.get().uri(finalUri).retrieve()
				.onStatus(status -> status.isError(), response -> {
					return response.bodyToMono(String.class).flatMap(body -> {
						return Mono.error(new RuntimeException("API 응답 오류(" + body + ")"));
					});
				}).bodyToMono(String.class).map(res -> {
					if (processedParams != null && processedParams.containsKey("dataType") && "xml".equalsIgnoreCase(String.valueOf(processedParams.get("dataType")))) {
						try {
							XmlMapper xmlMapper = new XmlMapper();
							return xmlMapper.readTree(res); 
						} catch (Exception e) {
							throw new RuntimeException("XML Parsing Error", e);
						}
					} else {
						if (res.trim().startsWith("<")) throw new RuntimeException("API 서버로부터 XML 에러 메시지 수신");
					}
					try {
						return objectMapper.readTree(res);
					} catch (Exception e) {
						throw new RuntimeException("JSON 파싱 오류", e);
					}
				}).block();
	}

	// 상세 API 호출
	private JsonNode fetchDetailFromApi(InterfaceSpec spec, String detailServiceId, Map<String, String> extractedKeys, Map<String, Object> processedParams) {
		String apiPath = String.format("/%s", detailServiceId); 
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
			
			builder.replaceQueryParam(PARAM_DETAIL_SERVICE_ID);
			builder.replaceQueryParam(PARAM_DETAIL_KEY);
			builder.replaceQueryParam(PARAM_DETAIL_ONLY_PARAMS);
			
			if (processedParams.containsKey(PARAM_MASTER_ONLY_PARAMS)) {
				String[] masterOnlyKeys = String.valueOf(processedParams.get(PARAM_MASTER_ONLY_PARAMS)).split(",");
				for (String key : masterOnlyKeys) {
					builder.replaceQueryParam(key.trim());
				}
				builder.replaceQueryParam(PARAM_MASTER_ONLY_PARAMS);
			}
		}
		
		for (Map.Entry<String, String> entry : extractedKeys.entrySet()) {
			builder.replaceQueryParam(entry.getKey(), entry.getValue());
		}

		java.net.URI finalUri = builder.build(false).encode(StandardCharsets.UTF_8).toUri();
		log.debug("상세 요청 API URL: {}", finalUri.toString());

		return openApiWebClient.get().uri(finalUri).retrieve()
				.onStatus(status -> status.isError(), response -> {
					return response.bodyToMono(String.class).flatMap(body -> Mono.error(new RuntimeException("상세 API 에러")));
				}).bodyToMono(String.class).map(res -> {
					if (processedParams != null && processedParams.containsKey("dataType") && "xml".equalsIgnoreCase(String.valueOf(processedParams.get("dataType")))) {
						try {
							XmlMapper xmlMapper = new XmlMapper();
							return xmlMapper.readTree(res);
						} catch (Exception e) {
							throw new RuntimeException("XML Parsing Error", e);
						}
					}
					try { return objectMapper.readTree(res); } 
					catch (Exception e) { throw new RuntimeException("상세 JSON 오류"); }
				}).block(); 
	}
}