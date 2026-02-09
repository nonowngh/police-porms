package mb.fw.policeporms.common.spec;

import java.util.Map;

import lombok.Data;
import mb.fw.policeporms.common.constant.ApiType;

@Data
public class InterfaceSpec {

	// 인터페이스 아이디
	private String interfaceId;
	// 인터페이스 명
	private String interfaceDescription;
	// open-api 메소드
//	private String apiMethod;
	// open-api url
	private String apiUrl;
	// open-api 요청 타임아웃(초)
	private int apiRequestTimeoutSeconds = 30;
	// open-api 타입
	private ApiType apiType;
//	// open-api 서비스 아이디
//	private String apiServiceId;
	// open-api 인증키
	private String apiKey;
	// open-api 호출건당 요청 사이즈
	private int apiRequestFetchSize = 1000;
	// 배치 스케줄러 크론
	private String batchSchedulerCron;
	// *테스트용 반복없이 한번만 호출
	private boolean loopCall = true;
	// 각 api 별로 추가 입력 정보
	private Map<String, Object> additionalParams;
	// 인터페이스 사용 여부
	private boolean enabled = true;
	
}
