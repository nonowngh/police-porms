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
import mb.fw.policeporms.domain.receiver.service.InterfaceProcessService;

@Slf4j
@ReceiverController
@RequestMapping(InterfaceApiPathConstants.INTERFACE_PATH)
public class InterfaceController {

	private final InterfaceProcessService interfaceProcessService;

	public InterfaceController(InterfaceProcessService interfaceProcessService) {
		this.interfaceProcessService = interfaceProcessService;
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
		return interfaceProcessService.fileProcess(request, file);
	}
}
