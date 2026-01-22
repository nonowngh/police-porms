package mb.fw.policeporms.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mb.fw.policeporms.dto.RequestMessage;
import mb.fw.policeporms.dto.ResponseMessage;

@RestController
@RequestMapping("/esb/api")
public class InnerController {

	@PostMapping("/inner/receive")
	public ResponseMessage innerReceive(@RequestBody RequestMessage request) {
		return ResponseMessage.builder().build();
	}
}
