package br.com.fiap.postech.soat16.fase1.exception;

public enum WorkOrderErrorCode implements ErrorCode {

    WORK_ORDER_NOT_FOUND,
    INVALID_STATUS_TRANSITION,
    WORK_ORDER_LOCKED,
    ESTIMATE_NOT_APPROVED;

    @Override
    public String getCode() {
        return name();
    }
}
