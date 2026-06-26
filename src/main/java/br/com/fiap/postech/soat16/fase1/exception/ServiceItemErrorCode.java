package br.com.fiap.postech.soat16.fase1.exception;

public enum ServiceItemErrorCode implements ErrorCode {

    SERVICE_ITEM_NOT_FOUND;

    @Override
    public String getCode() {
        return name();
    }
}
