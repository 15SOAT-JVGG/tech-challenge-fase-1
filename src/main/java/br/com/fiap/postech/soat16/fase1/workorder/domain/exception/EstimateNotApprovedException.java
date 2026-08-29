package br.com.fiap.postech.soat16.fase1.workorder.domain.exception;

import static br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorType.BUSINESS;
import static br.com.fiap.postech.soat16.fase1.workorder.domain.exception.WorkOrderErrorCode.ESTIMATE_NOT_APPROVED;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.AppException;

public class EstimateNotApprovedException extends AppException {

    public EstimateNotApprovedException() {
        super("Work order has no approved estimate", ESTIMATE_NOT_APPROVED, BUSINESS);
    }
}
