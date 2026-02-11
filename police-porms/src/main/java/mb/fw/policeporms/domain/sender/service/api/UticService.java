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
import java.util.ArrayList;
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
public class UticService extends AbstractApiService {

	protected UticService(ObjectMapper objectMapper, WebClient openApiWebClient) {
		super(objectMapper, openApiWebClient);
	}

	@Override
	public ApiType getApiType() {
		return ApiType.UTIC;
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
			if (root == null || root.isEmpty()) {
				throw new RuntimeException("[" + spec.getInterfaceId() + "}] Empty response from API");
			}

			int totalCount = getTotalSize(root);
			log.debug("'{}' api response total-count : {}", spec.getApiUrl(), totalCount);

			if (totalCount == 0)
				return 0;

			JsonNode recordNode = root.path(ApiResponseKeys.UTIC_RECORD.getValue());
			List<Map<String, Object>> rows = new ArrayList<>();
			if (recordNode.isArray()) {
				// 레코드가 여러 개인 경우
				rows = objectMapper.convertValue(recordNode, new TypeReference<List<Map<String, Object>>>() {
				});
			} else if (recordNode.isObject()) {
				// 레코드가 단 하나인 경우
				rows.add(objectMapper.convertValue(recordNode, new TypeReference<Map<String, Object>>() {
				}));
			}

			// 데이터 추출 및 파일 기록
			if (rows.size() > 0) {
				writeRowsToWriter(writer, rows);
				totalSaved = totalCount;
				LoggingUtils.printWriteFileComplete(transactionId, totalSaved);
			}

		} catch (IOException e) {
			log.error("파일 처리 중 오루 -> ", e);
			throw new RuntimeException(e);
		}
		return totalSaved;
	}

	private int getTotalSize(JsonNode root) {
		JsonNode recordNode = root.path(ApiResponseKeys.UTIC_RECORD.getValue());
		if (recordNode.isMissingNode()) {
			return 0;
		}
		if (recordNode.isArray()) {
			return recordNode.size();
		} else if (recordNode.isObject()) {
			return 1;
		}
		return 0;
	}

	private JsonNode callApi(InterfaceSpec spec) {
		return openApiWebClient.get().uri(WebClientUtils.appendQueryParams(spec, "").build().toUri()).retrieve()
				.onStatus(status -> status.isError(), response -> response.bodyToMono(String.class).flatMap(body -> {
					log.error("API 호출 에러 발생! 응답 바디: {}", body);
					return Mono.error(new RuntimeException("API 응답 오류(" + body + ")"));
				})).bodyToMono(String.class).map(xml -> {
					try {
						return xmlMapper.readTree(xml.getBytes(StandardCharsets.UTF_8));
					} catch (IOException e) {
						throw new RuntimeException("XML 파싱 오류", e);
					}
				}).block();
	}

}
