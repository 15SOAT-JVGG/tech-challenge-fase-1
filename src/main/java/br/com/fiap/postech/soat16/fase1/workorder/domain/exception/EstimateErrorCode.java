package br.com.fiap.postech.soat16.fase1.workorder.domain.exception;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorCode;

public enum EstimateErrorCode implements ErrorCode {

    ESTIMATE_NOT_FOUND,
    ESTIMATE_ALREADY_DECIDED,
    ESTIMATE_PART_NOT_FOUND,
    INSUFFICIENT_PART_STOCK,
    DECISION_TOKEN_INVALID,
    DECISION_TOKEN_EXPIRED,
    DECISION_TOKEN_ALREADY_USED;

    @Override
    public String getCode() {
        return name();
    }
}
