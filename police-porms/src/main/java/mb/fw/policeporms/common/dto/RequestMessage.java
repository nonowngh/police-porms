package mb.fw.policeporms.common.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class RequestMessage {

	// 인터페이스 아이디
	private String interfaceId;
	// 트랜젝션 아이디
	private String transactionId;
	// 데이터(파일로 전송할 예정이라 사용하지 않을 것 같음)
//	private List<Map<String, Object>> dataList;
	// 송신 파일 명
	private String sendFileName;
	// 송신 파일 크기
	private long sendFileSize;
	// 총 데이터 건수
	private int sendDataCount;

}
