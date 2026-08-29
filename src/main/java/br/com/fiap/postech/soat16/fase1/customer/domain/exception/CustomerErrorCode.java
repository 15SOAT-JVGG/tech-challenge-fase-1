package br.com.fiap.postech.soat16.fase1.customer.domain.exception;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorCode;

public enum CustomerErrorCode implements ErrorCode {

    CUSTOMER_NOT_FOUND,
    DOCUMENT_ALREADY_EXISTS,
    INVALID_DOCUMENT,
    CUSTOMER_HAS_VEHICLES;

    @Override
    public String getCode() {
        return name();
    }
}
