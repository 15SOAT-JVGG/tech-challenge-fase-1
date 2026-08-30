package br.com.fiap.postech.soat16.fase1.workorder.domain.exception;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorCode;

public enum WorkOrderErrorCode implements ErrorCode {

    WORK_ORDER_NOT_FOUND,
    INVALID_STATUS_TRANSITION,
    WORK_ORDER_LOCKED,
    ESTIMATE_NOT_APPROVED,
    TRACKING_TOKEN_INVALID,
    TRACKING_TOKEN_EXPIRED;

    @Override
    public String getCode() {
        return name();
    }
}
