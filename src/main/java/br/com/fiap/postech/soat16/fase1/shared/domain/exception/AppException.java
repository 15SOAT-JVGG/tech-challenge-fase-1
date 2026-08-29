package br.com.fiap.postech.soat16.fase1.shared.domain.exception;

public abstract class AppException extends RuntimeException {

    private final ErrorType type;
    private final String code;

    protected AppException(String message, ErrorCode errorCode, ErrorType errorType) {
        super(message);
        this.type = errorType;
        this.code = errorCode.getCode();
    }

    protected AppException(
            String message,
            ErrorCode errorCode,
            ErrorType errorType,
            Throwable cause
    ) {
        super(message, cause);
        this.type = errorType;
        this.code = errorCode.getCode();
    }

    public ErrorType getType() {
        return type;
    }

    public String getCode() {
        return code;
    }
}
