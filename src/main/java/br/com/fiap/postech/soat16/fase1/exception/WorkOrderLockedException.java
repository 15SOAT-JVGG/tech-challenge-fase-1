package br.com.fiap.postech.soat16.fase1.exception;

import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.BUSINESS;
import static br.com.fiap.postech.soat16.fase1.exception.WorkOrderErrorCode.WORK_ORDER_LOCKED;

public class WorkOrderLockedException extends AppException {

    public WorkOrderLockedException() {
        super("Work order is delivered or cancelled and cannot be modified", WORK_ORDER_LOCKED, BUSINESS);
    }
}
