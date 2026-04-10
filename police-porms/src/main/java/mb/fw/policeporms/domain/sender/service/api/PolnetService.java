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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import lombok.extern.slf4j.Slf4j;
import mb.fw.policeporms.common.annotation.SenderComponent;
import mb.fw.policeporms.common.constant.ApiResponseKeys;
import mb.fw.policeporms.common.constant.ApiType;
import mb.fw.policeporms.common.spec.InterfaceSpec;
import mb.fw.policeporms.common.utils.LoggingUtils;
import mb.fw.policeporms.domain.sender.service.base.AbstractApiService;
import mb.fw.atb.util.crypto.Seed128Cipher;
import reactor.core.publisher.Mono;

@Slf4j
@SenderComponent
public class PolnetService extends AbstractApiService {

    private static final int DEFAULT_TIMEOUT_SECONDS = 5; 
    private static final int MAX_RETRY_COUNT = 3;
    
    private static final String DEFAULT_SERVICE_ID = "PN02";
    private static final String DEFAULT_START_DTTM = "20000101000000";

    private static final String PARAM_SYSTEM_CODE = "systemCode";
    private static final String PARAM_SYSTEM_NAME = "systemName";
    private static final String PARAM_AUTH_KEY = "authKey";
    private static final String PARAM_ENCRYPTION_KEY = "encryptionKey";
    private static final String PARAM_QRY_CL = "qryCl";
    private static final String PARAM_START_DTTM = "startDttm";
    private static final String PARAM_END_DTTM = "endDttm";

    private final XmlMapper xmlMapper;

    protected PolnetService(ObjectMapper objectMapper, WebClient openApiWebClient) {
        super(objectMapper, openApiWebClient);
        this.xmlMapper = new XmlMapper();
    }

    @Override
    public ApiType getApiType() {
        return ApiType.POLNET;
    }

    @Override
    public int fetchAndSave(InterfaceSpec spec, Path tempFile, String transactionId) {
        Map<String, Object> additionalParams = spec.getAdditionalParams();
        
        if (additionalParams == null) {
            throw new IllegalArgumentException("폴넷 연계를 위한 파라미터가 없음");
        }
        if (!additionalParams.containsKey(PARAM_SYSTEM_CODE) || !additionalParams.containsKey(PARAM_AUTH_KEY) || !additionalParams.containsKey(PARAM_ENCRYPTION_KEY)) {
            throw new IllegalArgumentException(String.format("필수 인증 정보 누락 (필요: %s, %s, %s)", PARAM_SYSTEM_CODE, PARAM_AUTH_KEY, PARAM_ENCRYPTION_KEY));
        }

        String systemCode = String.valueOf(additionalParams.get(PARAM_SYSTEM_CODE));
        String systemName = String.valueOf(additionalParams.getOrDefault(PARAM_SYSTEM_NAME, "연계시스템"));
        String authKey = String.valueOf(additionalParams.get(PARAM_AUTH_KEY));
        String encryptionKey = String.valueOf(additionalParams.get(PARAM_ENCRYPTION_KEY));
        String qryCl = String.valueOf(additionalParams.getOrDefault(PARAM_QRY_CL, "1")); 
        String tlgrCd = (spec.getApiServiceId() != null && !spec.getApiServiceId().trim().isEmpty()) ? spec.getApiServiceId().toUpperCase() : DEFAULT_SERVICE_ID;

        // 날짜 동적 계산 로직 적용
        String startDttmParam = String.valueOf(additionalParams.getOrDefault(PARAM_START_DTTM, DEFAULT_START_DTTM));
        String startDttm;
        String endDttm;

        if ("DAILY_BATCH".equalsIgnoreCase(startDttmParam)) {
            // 일일 배치 모드: 어제 00:00:00 ~ 오늘 23:59:59
            startDttm = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd000000"));
            endDttm = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd235959"));
            log.info("[{}] 일일 배치 모드 동작. 조회 기간: {} ~ {}", spec.getInterfaceId(), startDttm, endDttm);
        } else {
            // 지정된 날짜 또는 기본값 사용
            startDttm = startDttmParam;
            if (additionalParams.containsKey(PARAM_END_DTTM) && additionalParams.get(PARAM_END_DTTM) != null) {
                endDttm = String.valueOf(additionalParams.get(PARAM_END_DTTM));
            } else {
                endDttm = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            }
        }

        int totalSaved = 0;
        int page = 1;
        // 0: 최초요청, 1: 추가요청
        String reqCl = "0"; 
        boolean hasNextPage = true;

        try (OutputStream fos = Files.newOutputStream(tempFile, StandardOpenOption.CREATE);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             GZIPOutputStream gzos = new GZIPOutputStream(bos);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzos, StandardCharsets.UTF_8))) {

            byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);

            while (hasNextPage) {
                // 공통 헤더 변수 선언 (요청 단위로 갱신되어야 하는 값)
                String reqstDttm = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
                String trnsId = reqstDttm + String.format("%05d", new Random().nextInt(100000));
                String qryPupsBase64 = Base64.getEncoder().encodeToString(systemName.getBytes(StandardCharsets.UTF_8));
                
                // XML 요청 전문 생성 (데이터 바디용 날짜인 startDttm, endDttm 주입)
                String plainXmlRequest = buildPolnetXmlRequest(trnsId, tlgrCd, reqstDttm, systemCode, authKey, qryPupsBase64, page, reqCl, qryCl, startDttm, endDttm);
                
                // SEED 128 암호화
                byte[] plainBytes = plainXmlRequest.getBytes(StandardCharsets.UTF_8);
                byte[] encryptedRequestBytes = Seed128Cipher.encrypt(plainBytes, keyBytes);
                
                // API 호출 (HTTP 헤더 7종 세팅 포함)
                byte[] encryptedResponseBytes = fetchPolnetApiWithRetry(spec, encryptedRequestBytes, trnsId, tlgrCd, reqstDttm, systemCode, authKey, qryPupsBase64);
                
                if (encryptedResponseBytes == null || encryptedResponseBytes.length == 0) {
                    log.warn("[{}] API로부터 빈 응답 수신 (페이지: {}). 페이징을 종료", spec.getInterfaceId(), page);
                    break;
                }

                // SEED 128 복호화
                byte[] decryptedBytes = Seed128Cipher.decrypt(encryptedResponseBytes, keyBytes);
                String xmlResponse = new String(decryptedBytes, StandardCharsets.UTF_8);

                // 응답 XML 파싱
                JsonNode rootNode = parseXmlToJsonNode(xmlResponse, spec.getInterfaceId());
                if (rootNode == null) break;

                JsonNode dataNode = rootNode.path(ApiResponseKeys.POLNET_DATA.getValue());
                if (dataNode.isMissingNode()) {
                    log.error("[{}] 응답에 <DATA> 태그가 없음", spec.getInterfaceId());
                    break;
                }

                String respCd = dataNode.path(ApiResponseKeys.POLNET_RESP_CD.getValue()).asText();

                // RECORD 추출 및 변환
                JsonNode recordNode = dataNode.path(ApiResponseKeys.POLNET_LOOP.getValue())
                                              .path(ApiResponseKeys.POLNET_RECORD.getValue());
                List<Map<String, Object>> rows = new ArrayList<>();

                if (!recordNode.isMissingNode()) {
                    try {
                        if (recordNode.isArray()) {
                            rows = objectMapper.convertValue(recordNode, new TypeReference<List<Map<String, Object>>>() {});
                        } else if (recordNode.isObject()) {
                            Map<String, Object> singleRow = objectMapper.convertValue(recordNode, new TypeReference<Map<String, Object>>() {});
                            rows.add(singleRow);
                        }
                    } catch (Exception e) {
                        log.error("[{}] JSON 노드를 Map으로 변환하는 중 오류 발생: {}", spec.getInterfaceId(), e.getMessage());
                        throw new RuntimeException("데이터 변환 오류", e);
                    }
                }

                // 파일 적재
                if (!rows.isEmpty()) {
                    writeRowsToWriter(writer, rows);
                    totalSaved += rows.size();
                    LoggingUtils.printWriteFileProgress(transactionId, rows.size(), totalSaved, -1);
                }

                // 페이징 로직
                if (ApiResponseKeys.POLNET_RESP_HAS_NEXT.getValue().equals(respCd)) {
                    page++;
                    reqCl = "1"; // 추가요청
                } else if (ApiResponseKeys.POLNET_RESP_SUCCESS.getValue().equals(respCd) || 
                           ApiResponseKeys.POLNET_RESP_NO_DATA.getValue().equals(respCd)) {
                    hasNextPage = false;
                } else {
                    log.error("[{}] 폴넷 시스템 예외 응답 수신 (코드: {})", spec.getInterfaceId(), respCd);
                    hasNextPage = false; 
                }

                if (!spec.isLoopCall()) {
                    break;
                }
            }

            LoggingUtils.printWriteFileComplete(transactionId, totalSaved);

        } catch (Exception e) {
            log.error("[{}] 폴넷 데이터 수집 중 오류 발생", spec.getInterfaceId(), e);
            try { Files.deleteIfExists(tempFile); } catch (IOException ex) { /* 무시 */ }
            throw new RuntimeException("폴넷 API 수집 연동 오류", e);
        }

        return totalSaved;
    }

    private JsonNode parseXmlToJsonNode(String xmlResponse, String interfaceId) {
        try {
            return xmlMapper.readTree(xmlResponse);
        } catch (JsonProcessingException e) {
            log.error("[{}] XML 파싱 실패. 응답 전문: {}", interfaceId, xmlResponse);
            return null;
        } catch (Exception e) {
            log.error("[{}] XML 처리 중 에러: {}", interfaceId, e.getMessage());
            return null;
        }
    }

    // HTTP Header에 7가지 항목을 주입하여 API 호출
    private byte[] fetchPolnetApiWithRetry(InterfaceSpec spec, byte[] requestBody, String trnsId, String tlgrCd, String reqstDttm, String systemCode, String authKey, String qryPupsBase64) {
        int attempt = 0;
        int timeoutSeconds = spec.getApiRequestTimeoutSeconds() > 0 ? spec.getApiRequestTimeoutSeconds() : DEFAULT_TIMEOUT_SECONDS;

        while (attempt < MAX_RETRY_COUNT) {
            try {
                return openApiWebClient.post()
                        .uri(spec.getApiUrl())
                        .contentType(MediaType.APPLICATION_XML) 
                        .header("TRNS_ID", trnsId)
                        .header("TLGR_CD", tlgrCd)
                        .header("REQST_DTTM", reqstDttm)
                        .header("REQST_SYS_CD", systemCode)
                        .header("AUTH_KEY", authKey)
                        .header("QRY_PUPS", qryPupsBase64)
                        .header("RESP_MSG", "") // 요청 시 해당 없음
                        .bodyValue(requestBody) 
                        .retrieve()
                        .onStatus(status -> status.isError(), response -> 
                            response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new RuntimeException(String.format("HTTP %s 오류: %s", response.statusCode(), body))))
                        )
                        .bodyToMono(byte[].class)
                        .block(Duration.ofSeconds(timeoutSeconds));
            } catch (Exception e) {
                attempt++;
                log.warn("⚠️ [{}] API 호출 실패 (재시도 {}/{}). 원인: {}", spec.getInterfaceId(), attempt, MAX_RETRY_COUNT, e.getMessage());
                if (attempt >= MAX_RETRY_COUNT) {
                    log.error("❌ [{}] 최대 재시도 횟수 초과.", spec.getInterfaceId());
                    throw e;
                }
                try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        return null;
    }

    // 주입받은 공통 헤더 변수 및 날짜 데이터를 사용하여 XML 생성
    private String buildPolnetXmlRequest(String trnsId, String tlgrCd, String reqstDttm, String systemCode, String authKey, String qryPupsBase64, int page, String reqCl, String qryCl, String startDttm, String endDttm) {
        
        return String.format(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<REQUEST>\n" +
            "  <HEADER>\n" +
            "    <TRNS_ID>%s</TRNS_ID>\n" +
            "    <TLGR_CD>%s</TLGR_CD>\n" +
            "    <REQST_DTTM>%s</REQST_DTTM>\n" +
            "    <REQST_SYS_CD>%s</REQST_SYS_CD>\n" +
            "    <AUTH_KEY>%s</AUTH_KEY>\n" +
            "    <QRY_PUPS>%s</QRY_PUPS>\n" +
            "    <REQ_CL>%s</REQ_CL>\n" +
            "    <QRY_CL>%s</QRY_CL>\n" +
            "    <RESP_CD />\n" +
            "    <REQ_CNT />\n" +
            "  </HEADER>\n" +
            "  <DATA>\n" +
            "    <PAGE>%d</PAGE>\n" +
            "    <CHG_START_DTTM>%s</CHG_START_DTTM>\n" +
            "    <CHG_END_DTTM>%s</CHG_END_DTTM>\n" +
            "  </DATA>\n" +
            "</REQUEST>",
            trnsId, 
            tlgrCd,
            reqstDttm, 
            systemCode,  
            authKey,      
            qryPupsBase64,
            reqCl, 
            qryCl, 
            page,
            startDttm, 
            endDttm
        );
    }
}