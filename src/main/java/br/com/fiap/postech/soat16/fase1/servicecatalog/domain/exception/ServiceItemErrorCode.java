package br.com.fiap.postech.soat16.fase1.servicecatalog.domain.exception;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorCode;

public enum ServiceItemErrorCode implements ErrorCode {

    SERVICE_ITEM_NOT_FOUND;

    @Override
    public String getCode() {
        return name();
    }
}
