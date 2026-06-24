package br.com.fiap.postech.soat16.fase1.exception;

import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.BUSINESS;
import static br.com.fiap.postech.soat16.fase1.exception.WorkOrderErrorCode.INVALID_STATUS_TRANSITION;

import br.com.fiap.postech.soat16.fase1.model.enums.WorkOrderStatus;

public class InvalidWorkOrderStatusTransitionException extends AppException {

    public InvalidWorkOrderStatusTransitionException(WorkOrderStatus from, WorkOrderStatus to) {
        super("Cannot transition work order from " + from + " to " + to, INVALID_STATUS_TRANSITION, BUSINESS);
    }

    public InvalidWorkOrderStatusTransitionException(String message) {
        super(message, INVALID_STATUS_TRANSITION, BUSINESS);
    }
}
