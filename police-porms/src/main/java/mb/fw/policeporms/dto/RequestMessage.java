package mb.fw.policeporms.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class RequestMessage {

	// 인터페이스 아이디
	private String interfaceId;
	// 트랜젝션 아이디
	private String transactionId;
	// 데이터
	private List<Map<String, Object>> dataList;
	// 총 데이터 건수
	private int dataCount = 0;

}
