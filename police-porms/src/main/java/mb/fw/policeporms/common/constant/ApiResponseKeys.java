package mb.fw.policeporms.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApiResponseKeys {
	
	//data-seoul 응답 정보
	DATA_SEOUL_TOTAL_COUNT("list_total_count"),
	DATA_SEOUL_RESULT("RESULT"),
	DATA_SEOUL_ROW_DATA("row"),
	DATA_SEOUL_RESULT_CODE("CODE"),
	DATA_SEOUL_RESULT_CODE_SUCCESS("INFO-000"),
	
	//data-portal 응답 정보
	DATA_PORTAL_TOTAL_COUNT("totalCount"),
	DATA_PORTAL_HEADER("header"),
	DATA_PORTAL_BODY("body"),
	DATA_PORTAL_ITEMS_DATA("items"),
	DATA_PORTAL_RESULT_CODE("resultCode"),
	DATA_PORTAL_RESULT_SUCCESS("00"),
	
	//its-center 응답 정보
	ITS_CENTER_HEADER("header"),
	ITS_CENTER_BODY("body"),
	ITS_CENTER_RESULT_CODE("resultCode"),
	ITS_CENTER_RESULT_MESSAGE("resultMsg"),
	ITS_CENTER_TOTAL_COUNT("totalCount"),
	ITS_CENTER_ITEMS("items"),
	ITS_CENTER_RESULT_CODE_SUCCESS("0");


    private final String value;
}