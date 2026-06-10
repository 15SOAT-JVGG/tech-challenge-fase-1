package br.com.fiap.postech.soat16.fase1.exception;

public enum AttendantErrorCode implements ErrorCode {

    ATTENDANT_NOT_FOUND,
    ATTENDANT_EMAIL_ALREADY_EXISTS,
    INVALID_ATTENDANT_CREDENTIALS,
    INACTIVE_ATTENDANT;

    @Override
    public String getCode() {
        return name();
    }
}
