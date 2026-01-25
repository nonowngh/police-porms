package mb.fw.policeporms.dto.openapi.서울시신호등관련정보;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Result {

	@JsonProperty("CODE")
	private String code;

	@JsonProperty("MESSAGE")
	private String message;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
