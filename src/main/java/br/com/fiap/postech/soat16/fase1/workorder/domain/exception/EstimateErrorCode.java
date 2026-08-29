package br.com.fiap.postech.soat16.fase1.workorder.domain.exception;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorCode;

public enum EstimateErrorCode implements ErrorCode {

    ESTIMATE_NOT_FOUND,
    ESTIMATE_ALREADY_DECIDED,
    ESTIMATE_PART_NOT_FOUND;

    @Override
    public String getCode() {
        return name();
    }
}
