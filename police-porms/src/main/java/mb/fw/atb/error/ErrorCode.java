package mb.fw.atb.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ErrorCode {

    DB_CONNECTION(100, "DB_CONNECTION", "데이터 베이스 연결에 관련된 오류"),
    INSERT_FAILURE(101, "INSERT_FAILURE", "데이터 베이스 Insert 오류"),
    UPDATE_FAILURE(102, "UPDATE_FAILURE", "데이터 베이스 Update 오류"),
    DELETE_FAILURE(103, "DELETE_FAILURE", "데이터 베이스 Update 오류"),
    SELECT_FAILURE(104, "SELECT_FAILURE", "데이터 베이스 Select 오류"),
    PROCEDURE_FAILURE(105, "PROCEDURE_FAILURE", "프로시저 호출 오류"),
    FILE_PERMISSION(201, "FILE_PERMISSION", "파일 권한에 관련된 오류"),
    FILE_NOT_FOUND(202, "FILE_NOT_FOUND", "파일을 찾을 수 없음"),
    FILE_GENERATE(203, "FILE_GENERATE", "파일 생성 오류"),
    VERIFICATION(901, "VERIFICATION", "검증에 관련된 오류"),
    JSON_PARSE(902, "JSON_PARSE", "Json 파싱 오류"),
    SCHEDULE_BATCH(903, "SCHEDULE_BATCH", "스케줄 배치 처리중 오류"),
    JMS_SEND(904, "JMS_SEND", "JMS 전송 오류"),
    ETC_ERROR(999, "ETC_ERROR", "기타 감지 되지 않은 예외 오류");

    private int status;
    private String code;
    private String message;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.message = message;
        this.code = code;
    }
}