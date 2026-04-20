package mb.fw.policeporms.domain.sender.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import mb.fw.policeporms.domain.sender.scheduler.DynamicScheduler;

@RestController
@RequestMapping("/api/interfaces")
@RequiredArgsConstructor
@Profile("sender")
public class InterfaceApiController {

    private final DynamicScheduler dynamicScheduler;

    @PostMapping("/{interfaceId}/execute")
    public ResponseEntity<String> executeInterface(@PathVariable String interfaceId) {
        
        boolean isStarted = dynamicScheduler.executeManually(interfaceId);
        
        if (isStarted) {
            return ResponseEntity.ok("인터페이스 [" + interfaceId + "] 수동 실행 요청이 접수되어 백그라운드에서 시작 됨");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("해당 인터페이스 ID [" + interfaceId + "] 를 찾을 수 없음 (interface-specs.json 확인 필요)");
        }
    }
}