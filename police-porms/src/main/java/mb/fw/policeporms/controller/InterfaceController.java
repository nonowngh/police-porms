package mb.fw.policeporms.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mb.fw.policeporms.constants.EsbApiPathConstants;
import mb.fw.policeporms.dto.RequestMessage;
import mb.fw.policeporms.dto.ResponseMessage;
import mb.fw.policeporms.service.InterfaceProcessService;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping(EsbApiPathConstants.INTERFACE_PATH)
public class InterfaceController {

	private final InterfaceProcessService interfaceProcessService;

	public InterfaceController(InterfaceProcessService interfaceProcessService) {
		this.interfaceProcessService = interfaceProcessService;
	}

	@PostMapping(EsbApiPathConstants.RECEIVE_DATA_PATH)
	public Mono<ResponseMessage> receiveData(@RequestBody Mono<RequestMessage> requestMono) {
		return requestMono.publishOn(Schedulers.boundedElastic()).map(interfaceProcessService::process);
	}
}
