package mb.fw.policeporms.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApiHeader {
	
	//data-seoul 헤더 정보
	DATA_SEOUL_TOTAL_COUNT("list_total_count"),
	DATA_SEOUL_RESULT("RESULT"),
	DATA_SEOUL_ROW_DATA("row"),
	DATA_SEOUL_RESULT_CODE("CODE"),
	DATA_SEOUL_RESULT_CODE_SUCCESS("INFO-000"),
	
	//data-portal 헤더 정보
	DATA_PORTAL_TOTAL_COUNT("totalCount"),
	DATA_PORTAL_HEADER("header"),
	DATA_PORTAL_BODY("body"),
	DATA_PORTAL_ITEMS_DATA("items"),
	DATA_PORTAL_RESULT_CODE("resultCode"),
	DATA_PORTAL_RESULT_SUCCESS("00");

    private final String value;
}