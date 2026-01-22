package mb.fw.policeporms.dto;

import lombok.Data;

@Data
public class RequestMessage {

	// 인터페이스 아이디
	private String interfaceId;
	// 트랜젝션 아이디
	private String transactionId;

}
