package mb.fw.policeporms.domain.receiver.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("local") // 반드시 local 환경에서만 실행되도록 설정
public class MockSftpServer {

    private SshServer sshd;
    
    // 로컬 가상 SFTP 최상위 디렉토리 (프로젝트 내 temp 폴더 활용)
    private static final String MOCK_ROOT_DIR = "/home/indigo";
    // 테스트용 계정 정보
    private static final String MOCK_USER = "seouluser01";
    private static final String MOCK_PASSWORD = "password"; // 평문 패스워드 (ENC 복호화 전 원본)
    private static final int MOCK_PORT = 2222;

    @PostConstruct
    public void startServer() {
        try {
            // 1. 가상 디렉토리 생성 및 더미 파일 세팅
            Path rootPath = Paths.get(MOCK_ROOT_DIR).toAbsolutePath();
            if (!Files.exists(rootPath)) {
                Files.createDirectories(rootPath);
            }
            
            // JSON 스펙에 정의된 원격지 경로 폴더 생성
            // /data_seoul/seouluser01/fpip_user/rtime_police3
            Path remoteDirPath = rootPath.resolve("rtime_police3");
            Files.createDirectories(remoteDirPath);
            
            // 다운로드 테스트를 위한 더미 ZIP 파일 생성 (파일명 동적 생성)
            createDummyZipFile(remoteDirPath);

            // SSHD 서버 기본 설정
            sshd = SshServer.setUpDefaultServer();
            sshd.setPort(MOCK_PORT);
            
            // 호스트 키 설정 (경고 방지용 임시 키)
            sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath()));

            // 사용자 인증 로직 (아이디/비번 체크)
            sshd.setPasswordAuthenticator((username, password, session) -> 
                MOCK_USER.equals(username) && MOCK_PASSWORD.equals(password)
            );

            // SFTP 서브시스템 활성화
            SftpSubsystemFactory factory = new SftpSubsystemFactory.Builder().build();
            sshd.setSubsystemFactories(Collections.singletonList(factory));

            // 파일 시스템 팩토리 설정 (SFTP 접속 시 MOCK_ROOT_DIR을 루트(/)로 인식하게 함)
            sshd.setFileSystemFactory(new VirtualFileSystemFactory(rootPath));

            // 서버 시작
            sshd.start();
            log.info("🚀 [Mock] 내장형 SFTP 서버가 시작되었습니다. (Port: {}, Root: {})", MOCK_PORT, rootPath);

        } catch (Exception e) {
            log.error("❌ [Mock] SFTP 서버 구동 중 오류 발생", e);
        }
    }

    @PreDestroy
    public void stopServer() {
        try {
            if (sshd != null && sshd.isStarted()) {
                sshd.stop();
                log.info("🛑 [Mock] 내장형 SFTP 서버를 종료합니다.");
            }
        } catch (IOException e) {
            log.error("Mock SFTP 서버 종료 오류", e);
        }
    }

    // 테스트를 위해 KtPopService가 다운로드할 수 있는 빈 ZIP 파일을 만드는 헬퍼 메서드
    private void createDummyZipFile(Path targetDir) throws IOException {
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String fileName = "dp_rtime_ftppl_50_cell_police3_sum_" + timeStr + ".zip";
        File zipFile = targetDir.resolve(fileName).toFile();

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            // ZIP 파일 내부에 더미 텍스트 파일 하나 추가
            ZipEntry entry = new ZipEntry("dummy_data.json");
            zos.putNextEntry(entry);
            zos.write("{\"message\": \"This is a mock KT POP data file\"}".getBytes());
            zos.closeEntry();
        }
        
        log.info("📦 [Mock] 테스트용 더미 파일 생성 완료: {}", zipFile.getAbsolutePath());
    }
}