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
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import mb.fw.policeporms.common.annotation.SenderComponent;
import mb.fw.policeporms.common.constant.ApiHeader;
import mb.fw.policeporms.common.constant.ApiParamKeys;
import mb.fw.policeporms.common.constant.ApiType;
import mb.fw.policeporms.common.spec.InterfaceSpec;
import mb.fw.policeporms.common.utils.LoggingUtils;
import mb.fw.policeporms.domain.sender.service.base.AbstractApiService;
import reactor.core.publisher.Mono;

@Slf4j
@SenderComponent
public class DataSeoulService extends AbstractApiService {

	protected DataSeoulService(ObjectMapper objectMapper, WebClient openApiWebClient) {
		super(objectMapper, openApiWebClient);
	}

	@Override
	public ApiType getApiType() {
		return ApiType.DATA_SEOUL;
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
				JsonNode root = fetchPageFromApi(spec, start, end);
				if (root == null || !root.fieldNames().hasNext()) {
					log.warn("[{}] Empty response from API at page {}", spec.getInterfaceId(), page);
					break;
				}
				String serviceKey = root.fieldNames().next();
				JsonNode serviceBody = root.get(serviceKey);
				log.debug("'{}' api response result : {}, total-count : {}",
						spec.getAdditionalParams().get(ApiParamKeys.SERVICE_ID), getResult(serviceBody).toString(),
						getTotalSize(serviceBody));
				// 에러 코드 체크 (INFO-000이 아니면 중단)
				String resultCode = getResult(serviceBody).path(ApiHeader.DATA_SEOUL_RESULT_CODE.getValue()).asText();
				if (!ApiHeader.DATA_SEOUL_RESULT_CODE_SUCCESS.getValue().equals(resultCode)) {
					log.error("[{}] API error code: {} at page {}", spec.getInterfaceId(), resultCode, page);
					break;
				}
				// 총 갯수 저장
				if (totalCount == -1) {
					totalCount = getTotalSize(serviceBody);
				}

				// 데이터 추출 및 파일 기록
				JsonNode rowNode = serviceBody.get(ApiHeader.DATA_SEOUL_ROW_DATA.getValue());
				if (rowNode != null && rowNode.isArray()) {
					List<Map<String, Object>> rows = objectMapper.convertValue(rowNode,
							new TypeReference<List<Map<String, Object>>>() {
							});

					// 파일 적재 (Gzip 스트림에 작성됨)
					writeRowsToWriter(writer, rows);
					totalSaved += rows.size();
					LoggingUtils.printWriteFileProgress(transactionId, rows.size(), totalSaved, totalCount);
//					log.debug("[{}] {} records saved to file (current:{}/total:{})", spec.getInterfaceId(), rows.size(),
//							totalSaved, totalCount);

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
		return serviceBody.path(ApiHeader.DATA_SEOUL_TOTAL_COUNT.getValue()).asInt();
	}

	private JsonNode getResult(JsonNode serviceBody) {
		return serviceBody.path(ApiHeader.DATA_SEOUL_RESULT.getValue());
	}

	private JsonNode fetchPageFromApi(InterfaceSpec spec, int start, int end) {
		String apiPath = String.format("/%s/json/%s/%d/%d/", spec.getApiKey(),
				spec.getAdditionalParams().get(ApiParamKeys.SERVICE_ID), start, end);
		return openApiWebClient.get().uri(spec.getApiUrl() + apiPath).retrieve()
				.onStatus(status -> status.isError(), response -> {
					return response.bodyToMono(String.class).flatMap(body -> {
						log.error("API 호출 에러 발생! 응답 바디: {}", body);
						return Mono.error(new RuntimeException("API 응답 오류"));
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
