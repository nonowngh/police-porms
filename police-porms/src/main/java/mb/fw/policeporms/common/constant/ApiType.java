package mb.fw.policeporms.common.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApiType {
	
	@JsonProperty("data-seoul")
	DATA_SEOUL("data-seoul", "서울시 데이터 광장"),
	
	@JsonProperty("data-portal")
	DATA_PORTAL("data-portal", "공공데이터 포털");
	
	private final String value;
	private final String description; 

	@JsonCreator // JSON 로딩 시 이 메서드를 통해 Enum을 생성함
    public static ApiType from(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        for (ApiType type : ApiType.values()) {
            if (type.value.equalsIgnoreCase(s)) {
                return type;
            }
        }
        return null; // 알 수 없는 값일 때 에러 대신 null 처리
    }
}