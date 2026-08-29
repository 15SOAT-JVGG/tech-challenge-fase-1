package br.com.fiap.postech.soat16.fase1.workorder.domain.exception;

import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.CONFLICT;
import static br.com.fiap.postech.soat16.fase1.workorder.domain.exception.EstimateErrorCode.ESTIMATE_ALREADY_DECIDED;

import br.com.fiap.postech.soat16.fase1.exception.AppException;

public class EstimateAlreadyDecidedException extends AppException {

    public EstimateAlreadyDecidedException() {
        super("Estimate has already been approved or rejected", ESTIMATE_ALREADY_DECIDED, CONFLICT);
    }
}
