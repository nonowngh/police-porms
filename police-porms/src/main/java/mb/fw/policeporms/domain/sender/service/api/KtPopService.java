package mb.fw.policeporms.domain.sender.service.api;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import lombok.extern.slf4j.Slf4j;
import mb.fw.policeporms.common.annotation.SenderComponent;
import mb.fw.policeporms.common.constant.ApiType;
import mb.fw.policeporms.common.spec.InterfaceSpec;
import mb.fw.policeporms.common.utils.ESBProductEncryption;
import mb.fw.policeporms.domain.sender.service.base.AbstractApiService;

@Slf4j
@SenderComponent
public class KtPopService extends AbstractApiService {

    private static final String PARAM_HOST = "host";
    private static final String PARAM_PORT = "port";
    private static final String PARAM_USERNAME = "username";
    private static final String PARAM_PASSWORD = "password";
    private static final String PARAM_REMOTE_DIR = "remoteDir";
    private static final String PARAM_FILE_PATTERN = "fileNamePattern";

    protected KtPopService(ObjectMapper objectMapper, WebClient openApiWebClient) {
        super(objectMapper, openApiWebClient);
    }

    @Override
    public ApiType getApiType() {
        return ApiType.KTPOP;
    }

    @Override
    public int fetchAndSave(InterfaceSpec spec, Path tempFile, String transactionId) {
        Map<String, Object> params = spec.getAdditionalParams();
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("[" + spec.getInterfaceId() + "] SFTP 접속 파라미터가 없습니다.");
        }

        String host = String.valueOf(params.getOrDefault(PARAM_HOST, "14.63.143.72"));
        int port = Integer.parseInt(String.valueOf(params.getOrDefault(PARAM_PORT, "2222")));
        String username = String.valueOf(params.get(PARAM_USERNAME));
        String password = String.valueOf(params.get(PARAM_PASSWORD));
        String remoteDir = String.valueOf(params.get(PARAM_REMOTE_DIR));
        String fileNamePattern = String.valueOf(params.get(PARAM_FILE_PATTERN));

        // 비밀번호 복호화 처리
        if (password.startsWith("ENC(") && password.endsWith(")")) {
            try {
                password = ESBProductEncryption.decryptString(password);
            } catch (Exception e) {
                log.error("[{}] SFTP 비밀번호 복호화 오류", spec.getInterfaceId(), e);
                throw new RuntimeException("SFTP 비밀번호 복호화 실패", e);
            }
        }

        // 동적 파일명 패턴을 정규식(Regex)으로 변환(예: "dp_rtime_..._YYYYmmddHHMM.zip" -> "^dp_rtime_..._\\d{12}\\.zip$")
        String regexPattern = "^" + fileNamePattern
                .replace(".", "\\.")
                .replace("YYYYmmddHHMM", "\\d{12}")
                + "$";

        log.info("[{}] SFTP 접속 준비 - 대상 서버: {}:{}, 경로: {}, 패턴: {}", 
                 spec.getInterfaceId(), host, port, remoteDir, regexPattern);

        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            // SFTP 세션 연결
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect(spec.getApiRequestTimeoutSeconds() * 1000);

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            channelSftp.cd(remoteDir);
            
            // 디렉토리 내 파일 목록을 조회하여 정규식과 매칭
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> fileList = channelSftp.ls(".");
            List<ChannelSftp.LsEntry> matchedFiles = new ArrayList<>();
            
            for (ChannelSftp.LsEntry entry : fileList) {
                // 디렉토리가 아니고, 정규식 패턴에 맞는 파일만 수집
                if (!entry.getAttrs().isDir() && entry.getFilename().matches(regexPattern)) {
                    matchedFiles.add(entry);
                }
            }

            // 매칭되는 파일이 없는 경우 안전하게 0 리턴
            if (matchedFiles.isEmpty()) {
                log.warn("[{}] SFTP 경로에 패턴({})과 일치하는 파일이 존재하지 않습니다.", spec.getInterfaceId(), fileNamePattern);
                return 0; 
            }

            // 매칭된 파일들 중 가장 최신 파일(파일명 내림차순) 선택
            matchedFiles.sort((f1, f2) -> f2.getFilename().compareTo(f1.getFilename()));
            String targetFileName = matchedFiles.get(0).getFilename();
            spec.getAdditionalParams().put("ORIGINAL_FILE_NAME", targetFileName);
            log.info("[{}] SFTP 원격지 파일 다운로드 대상 확정: {} (총 매칭 {}건 중 최신)", spec.getInterfaceId(), targetFileName, matchedFiles.size());
            
            // 파일 다운로드
            try (InputStream is = channelSftp.get(targetFileName);
                 FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
                
                byte[] buffer = new byte[8192];
                int read;
                int totalBytes = 0;
                
                while ((read = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, read);
                    totalBytes += read;
                }
                
                log.info("[{}] SFTP 파일 다운로드 완료. 총 {} bytes 저장됨", spec.getInterfaceId(), totalBytes);
                return 1; // 단일 파일 다운로드 성공 의미
            }

        } catch (SftpException e) {
            log.error("[{}] SFTP 원격지 경로 접근 실패 (경로: {}): {}", spec.getInterfaceId(), remoteDir, e.getMessage(), e);
            throw new RuntimeException("SFTP 경로 접근 실패", e);
        } catch (Exception e) {
            log.error("[{}] SFTP 통신 중 오류 발생", spec.getInterfaceId(), e);
            throw new RuntimeException("SFTP 다운로드 통신 실패", e);
        } finally {
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}