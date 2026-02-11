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
import mb.fw.policeporms.common.constant.ApiResponseKeys;
import mb.fw.policeporms.common.constant.ApiType;
import mb.fw.policeporms.common.spec.InterfaceSpec;
import mb.fw.policeporms.common.utils.LoggingUtils;
import mb.fw.policeporms.common.utils.WebClientUtils;
import mb.fw.policeporms.domain.sender.service.base.AbstractApiService;
import reactor.core.publisher.Mono;

@Slf4j
@SenderComponent
public class ItsCenterService extends AbstractApiService {

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
		try (OutputStream fos = Files.newOutputStream(tempFile, StandardOpenOption.CREATE);
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				GZIPOutputStream gzos = new GZIPOutputStream(bos); // 압축 레이어 추가
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzos, StandardCharsets.UTF_8))) {

			// api 호출
			JsonNode root = callApi(spec);
			if (root == null || !root.fieldNames().hasNext()) {
				throw new RuntimeException("[" + spec.getInterfaceId() + "}] Empty response from API");
			}
			JsonNode headerNode = root.path(ApiResponseKeys.ITS_CENTER_HEADER.getValue());
			JsonNode bodyNode = root.path(ApiResponseKeys.ITS_CENTER_BODY.getValue());
			int totalCount = getTotalSize(bodyNode);
			log.debug("'{}' api response result : {}, total-count : {}", spec.getApiServiceId(), headerNode.toString(),
					totalCount);

			if (totalCount == 0)
				return 0;

			// 에러 코드 체크 ('0'이 아니면 중단)
			String resultCode = getResult(headerNode).asText();
			if (!ApiResponseKeys.ITS_CENTER_RESULT_CODE_SUCCESS.getValue().equals(resultCode)) {
				throw new RuntimeException("[" + spec.getInterfaceId() + "] API error : " + headerNode.asText());
			}

			// 데이터 추출 및 파일 기록
			JsonNode rowNode = bodyNode.get(ApiResponseKeys.ITS_CENTER_ITEMS.getValue());
			if (rowNode != null && rowNode.isArray()) {
				List<Map<String, Object>> rows = objectMapper.convertValue(rowNode,
						new TypeReference<List<Map<String, Object>>>() {
						});

				// 파일 적재 (Gzip 스트림에 작성됨)
				writeRowsToWriter(writer, rows);
				totalSaved += rows.size();
				LoggingUtils.printWriteFileComplete(transactionId, rows.size());
			}

		} catch (IOException e) {
			log.error("파일 처리 중 오루 -> ", e);
			throw new RuntimeException(e);
		}
		return totalSaved;
	}

	private int getTotalSize(JsonNode bodyNode) {
		return bodyNode.path(ApiResponseKeys.ITS_CENTER_TOTAL_COUNT.getValue()).asInt();
	}

	private JsonNode getResult(JsonNode headerNode) {
		return headerNode.path(ApiResponseKeys.ITS_CENTER_RESULT_CODE.getValue());
	}

	private JsonNode callApi(InterfaceSpec spec) {
		String apiPath = String.format("/%s", spec.getApiServiceId());

		return openApiWebClient.get().uri(WebClientUtils.appendQueryParams(spec, apiPath).build().toUri()).retrieve()
				.onStatus(status -> status.isError(), response -> {
					return response.bodyToMono(String.class).flatMap(body -> {
						log.error("API 호출 에러 발생! 응답 바디: {}", body);
						return Mono.error(new RuntimeException("API 응답 오류(" + body + ")"));
					});
				}).bodyToMono(String.class).map(res -> {
					try {
						return objectMapper.readTree(res);
					} catch (Exception e) {
						throw new RuntimeException("JSON 파싱 오류", e);
					}
				}).block();
	}

}
