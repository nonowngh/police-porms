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
public class GeonurisService extends AbstractApiService {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int MAX_RETRY_COUNT = 3;

    protected GeonurisService(ObjectMapper objectMapper, WebClient openApiWebClient) {
        super(objectMapper, openApiWebClient);
    }

    @Override
    public ApiType getApiType() {
        return ApiType.JURISD;
    }

    @Override
    public int fetchAndSave(InterfaceSpec spec, Path tempFile, String transactionId) {
        int totalSaved = 0;

        try (OutputStream fos = Files.newOutputStream(tempFile, StandardOpenOption.CREATE);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             GZIPOutputStream gzos = new GZIPOutputStream(bos);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzos, StandardCharsets.UTF_8))) {

            JsonNode root = fetchApiWithRetry(spec);

            if (root == null || !root.has(ApiResponseKeys.JURISD_FEATURES.getValue())) {
                log.warn("[{}] 응답에 features 데이터가 없거나 비어있습니다.", spec.getInterfaceId());
                return totalSaved;
            }

            JsonNode rowNode = makeJsonFlattener(root);

            if (rowNode != null && rowNode.isArray()) {
                List<Map<String, Object>> rows = objectMapper.convertValue(rowNode, new TypeReference<List<Map<String, Object>>>() {});

                if (!rows.isEmpty()) {
                    writeRowsToWriter(writer, rows);
                    totalSaved = rows.size();
                    
                    LoggingUtils.printWriteFileProgress(transactionId, totalSaved, totalSaved, totalSaved); 
                }
            }

            LoggingUtils.printWriteFileComplete(transactionId, totalSaved);

        } catch (Exception e) {
            log.error("[{}] 데이터 처리 중 오류 발생, 생성 중인 파일 삭제 시도 -> ", spec.getInterfaceId(), e);
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ex) {
                log.error("[{}] 임시 파일 삭제 실패: {}", spec.getInterfaceId(), tempFile, ex);
            }
            throw new RuntimeException("API 데이터 적재 중 오류 발생", e);
        }
        
        return totalSaved;
    }

    private JsonNode fetchApiWithRetry(InterfaceSpec spec) {
        int attempt = 0;
        while (attempt < MAX_RETRY_COUNT) {
            try {
                return fetchFromApi(spec);
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

    private JsonNode fetchFromApi(InterfaceSpec spec) {
        String apiPath = String.format("/%s", spec.getApiServiceId());
        UriComponentsBuilder builder = WebClientUtils.appendQueryParams(spec, apiPath);

        int timeoutSeconds = spec.getApiRequestTimeoutSeconds() > 0 ? spec.getApiRequestTimeoutSeconds() : DEFAULT_TIMEOUT_SECONDS;

        return openApiWebClient.get().uri(builder.build().toUri()).retrieve()
                .onStatus(status -> status.isError(), response -> {
                    return response.bodyToMono(String.class).flatMap(body -> Mono.error(new RuntimeException("API 응답 오류(" + body + ")")));
                }).bodyToMono(String.class).map(res -> {
                    if (res.trim().startsWith("<")) throw new RuntimeException("API 서버로부터 XML 에러 메시지 수신: " + res);
                    try {
                        return objectMapper.readTree(res);
                    } catch (Exception e) {
                        throw new RuntimeException("JSON 파싱 오류", e);
                    }
                }).block(Duration.ofSeconds(timeoutSeconds));
    }

    private JsonNode makeJsonFlattener(JsonNode root) {
        try {
            JsonNode featuresNode = root.get(ApiResponseKeys.JURISD_FEATURES.getValue());
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

                    if (ApiResponseKeys.JURISD_FEATURE_PROPERTIES.getValue().equals(key)) {
                        if (value != null && value.isObject()) {
                            Iterator<Map.Entry<String, JsonNode>> props = value.fields();
                            while (props.hasNext()) {
                                Map.Entry<String, JsonNode> prop = props.next();
                                flattenedFeature.set(prop.getKey(), prop.getValue());
                            }
                        }
                    } 
                    else if (ApiResponseKeys.JURISD_FEATURE_GEOMETRY.getValue().equals(key)) {
                        if (value != null && !value.isNull()) {
                            flattenedFeature.put(key, value.toString());
                        } else {
                            flattenedFeature.set(key, value);
                        }
                    } 
                    else {
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