package mb.fw.policeporms.receiver.exception;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;
import mb.fw.policeporms.constants.InterfaceStatusConstants;
import mb.fw.policeporms.dto.ResponseMessage;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ResponseMessage> handleAllException(Exception e) {
		// MDC에서 값 추출 (컨트롤러에서 넣어둔 값)
        String interfaceId = MDC.get("interfaceId");
        String transactionId = MDC.get("transactionId");

        // 혹시라도 MDC에 값이 없는 경우를 대비한 기본값
        interfaceId = (interfaceId != null) ? interfaceId : "UNKNOWN";
        transactionId = (transactionId != null) ? transactionId : "UNKNOWN";

        log.error("[{}-{}] 수신 서버 에러 발생: {}", interfaceId, transactionId, e.getMessage(), e);
		ResponseMessage response = new ResponseMessage();
		response.setInterfaceId(interfaceId);
        response.setTransactionId(transactionId);
        response.setResultCount(0);
		response.setProcessCd(InterfaceStatusConstants.ERROR);
		response.setProcessMsg("수신 처리 오류 : " + e.getMessage());

		MDC.clear();
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}
}
