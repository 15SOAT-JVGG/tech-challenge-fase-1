package br.com.fiap.postech.soat16.fase1.exception;

import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.BUSINESS;
import static br.com.fiap.postech.soat16.fase1.exception.WorkOrderErrorCode.ESTIMATE_NOT_APPROVED;

public class EstimateNotApprovedException extends AppException {

    public EstimateNotApprovedException() {
        super("Work order has no approved estimate", ESTIMATE_NOT_APPROVED, BUSINESS);
    }
}
