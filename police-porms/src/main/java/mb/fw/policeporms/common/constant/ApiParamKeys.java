package mb.fw.policeporms.common.constant;

public class ApiParamKeys {
	private ApiParamKeys() {
	}

	// 공통 사용
	public static final String COMMON_SERVICE_ID = "serviceId";
	public static final String COMMON_API_KEY = "apiKey";

	// 데이터 포털 파라미터
	public static final String AREA_NO = "areaNo";
	public static final String TIME = "time";
	public static final String LOCDATE = "locdate";
	public static final String LOCATION = "location";

	// 국가교통정보센터 파라미터
	// its-center 서비스에서 사용하는 API TYPE
	public static final String ITS_CENTER_API_TYPE = "type";
	// its-center 서비스에서 사용하는 출력형식 TYPE
	public static final String ITS_CENTER_OUTPUT_TYPE = "getType";
	public static final String ITS_CENTER_MIN_X = "minX";
	public static final String ITS_CENTER_MAX_X = "maxX";
	public static final String ITS_CENTER_MIN_Y = "minY";
	public static final String ITS_CENTER_MAX_Y = "maxY";

}
