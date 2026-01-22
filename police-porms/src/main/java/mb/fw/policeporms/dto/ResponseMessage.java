package mb.fw.policeporms.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseMessage {

	// 인터페이스 아이디
	private String interfaceId;
	// 트랜젝션 아이디
	private String transactionId;
	// 처리 코드
	private String processCd;
	// 처리 메시지
	private String processMsg;
}
