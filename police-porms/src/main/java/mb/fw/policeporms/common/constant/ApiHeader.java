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
	DATA_SEOUL_RESULT_CODE_SUCCESS("INFO-000");

    private final String value;
}