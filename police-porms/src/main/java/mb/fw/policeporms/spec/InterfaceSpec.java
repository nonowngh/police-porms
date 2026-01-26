package mb.fw.policeporms.spec;

import lombok.Data;

@Data
public class InterfaceSpec {

	//인터페이스 아이디
	private String interfaceId;
	//인터페이스 명
	private String interfaceDescription;
	//open-api 메소드
	private String apiMethod;
	//open-api url
	private String apiUrl;
	//open-api 요청 타임아웃(초)
	private int apiRequestTimeoutSeconds = 30;
	//open-api 구분 명칭
	private String apiName;
	//배치 스케줄러 크론
	private String batchSchedulerCron;
}
