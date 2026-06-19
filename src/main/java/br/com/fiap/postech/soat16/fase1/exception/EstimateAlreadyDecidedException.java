package br.com.fiap.postech.soat16.fase1.exception;

import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.CONFLICT;
import static br.com.fiap.postech.soat16.fase1.exception.EstimateErrorCode.ESTIMATE_ALREADY_DECIDED;

public class EstimateAlreadyDecidedException extends AppException {

    public EstimateAlreadyDecidedException() {
        super("Estimate has already been approved or rejected", ESTIMATE_ALREADY_DECIDED, CONFLICT);
    }
}
