package mb.fw.policeporms.domain.receiver.controller;

import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import mb.fw.policeporms.common.annotation.ReceiverController;
import mb.fw.policeporms.common.constant.InterfaceApiPathConstants;
import mb.fw.policeporms.common.dto.RequestMessage;
import mb.fw.policeporms.common.dto.ResponseMessage;
import mb.fw.policeporms.domain.receiver.service.InterfaceFileSaveService;
import mb.fw.policeporms.domain.receiver.service.InterfaceProcessService;

@Slf4j
@ReceiverController
@RequestMapping(InterfaceApiPathConstants.INTERFACE_PATH)
public class InterfaceController {

	private final InterfaceProcessService interfaceProcessService;
	private final InterfaceFileSaveService interfaceFileSaveService;

	public InterfaceController(InterfaceProcessService interfaceProcessService,
			InterfaceFileSaveService interfaceFileSaveService) {
		this.interfaceProcessService = interfaceProcessService;
		this.interfaceFileSaveService = interfaceFileSaveService;
	}

//	@PostMapping(EsbApiPathConstants.RECEIVE_DATA_PATH)
//	public Mono<ResponseMessage> receiveData(@RequestBody Mono<RequestMessage> requestMono) {
//		return requestMono.publishOn(Schedulers.boundedElastic()).map(interfaceProcessService::process);
//	}

	@PostMapping(InterfaceApiPathConstants.RECEIVE_FILE_PATH)
	public ResponseMessage receiveFile(@RequestPart("message") RequestMessage request,
			@RequestPart("file") MultipartFile file // 파일 파트
	) {
		log.info("[{}] 요청 수신 - 파일명: {}", request.getTransactionId(), request.getSendFileName());
		MDC.put("interfaceId", request.getInterfaceId());
		MDC.put("transactionId", request.getTransactionId());
		
		ResponseMessage response;

        // JSON 스펙에서 주입받은 파라미터를 기반으로 동적 라우팅
        String processType = request.getReceiverProcessType();

        if ("FILE".equalsIgnoreCase(processType)) {
            // 물리적 파일 시스템에 저장하는 전용 서비스 호출
            response = interfaceFileSaveService.saveFileToDirectory(request, file);
        } else {
            // DB 파싱 및 INSERT를 수행하는 기존 서비스 (기본값)
            response = interfaceProcessService.fileProcess(request, file);
        }

        return response;
	}
}
