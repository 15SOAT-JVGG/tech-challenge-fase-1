package br.com.fiap.postech.soat16.fase1.exception;

public enum CustomerErrorCode implements ErrorCode {

    CUSTOMER_NOT_FOUND,
    DUPLICATE_CUSTOMER,
    PHONE_ALREADY_EXISTS,
    EMAIL_ALREADY_EXISTS;

    @Override
    public String getCode() {
        return name();
    }
}
