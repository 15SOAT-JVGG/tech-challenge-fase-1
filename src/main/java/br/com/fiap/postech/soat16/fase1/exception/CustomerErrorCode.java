package br.com.fiap.postech.soat16.fase1.exception;

public enum CustomerErrorCode implements ErrorCode {

    CUSTOMER_NOT_FOUND,
    DOCUMENT_ALREADY_EXISTS,
    INVALID_DOCUMENT;

    @Override
    public String getCode() {
        return name();
    }
}
