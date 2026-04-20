package mb.fw.policeporms.domain.receiver.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import mb.fw.policeporms.common.annotation.ReceiverService;
import mb.fw.policeporms.common.constant.InterfaceStatus;
import mb.fw.policeporms.common.dto.RequestMessage;
import mb.fw.policeporms.common.dto.ResponseMessage;

@Slf4j
@ReceiverService
public class InterfaceFileSaveService {

    private String baseReceiveDir;

    @Autowired
    private Environment env;

    @PostConstruct
    public void init() {
        this.baseReceiveDir = env.getProperty("fileif.receiver.base-dir", "/tmp/policeporms/receive");
        log.info("✅ [수신 파일 저장 디렉토리] : {}", this.baseReceiveDir);
    }

    public ResponseMessage saveFileToDirectory(RequestMessage request, MultipartFile file) {
        String interfaceId = request.getInterfaceId();
        String transactionId = request.getTransactionId();
        
        String targetFileName = request.getSendFileName(); 

        ResponseMessage response = new ResponseMessage();
        response.setInterfaceId(interfaceId);
        response.setTransactionId(transactionId);

        if (file == null || file.isEmpty()) {
            log.error("[{}] 수신된 파일이 비어있거나 존재하지 않습니다.", transactionId);
            response.setProcessCd(InterfaceStatus.ERROR);
            response.setProcessMsg("전송된 파일이 존재하지 않습니다.");
            response.setResultCount(0);
            return response;
        }

        try {
            Path targetDir = Paths.get(baseReceiveDir, interfaceId);
            
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            Path targetFilePath = targetDir.resolve(targetFileName);

            Files.copy(file.getInputStream(), targetFilePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("[{}] 물리적 파일 저장 완료: {}", transactionId, targetFilePath.toAbsolutePath());

            response.setProcessCd(InterfaceStatus.SUCCESS);
            response.setProcessMsg("파일 저장 성공");
            response.setResultCount(1);

        } catch (IOException e) {
            log.error("[{}] 수신 파일 디렉토리 저장 중 치명적 오류 발생", transactionId, e);
            response.setProcessCd(InterfaceStatus.ERROR);
            response.setProcessMsg("디렉토리 파일 저장 실패 : " + e.getMessage());
            response.setResultCount(0);
        }

        return response;
    }
}