package mb.fw.atb.error;

public class ATBException extends RuntimeException {

    private ErrorCode errorCode;
    private String returnJson;

    public ATBException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ATBException(String message, ErrorCode errorCode, Throwable cause, String returnJson) {
        super(message, cause);
        this.errorCode = errorCode;
        this.returnJson = returnJson;
    }

    public ErrorCode getErrorCode() {
        return this.errorCode;
    }

    public String getReturnJson() {
        if(returnJson == null) {
            returnJson = "{}";
        }
        return this.returnJson;
    }
}
