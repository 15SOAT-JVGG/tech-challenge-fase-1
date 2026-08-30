package br.com.fiap.postech.soat16.fase1.workorder.domain.exception;

import static br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorType.GONE;
import static br.com.fiap.postech.soat16.fase1.workorder.domain.exception.EstimateErrorCode.DECISION_TOKEN_EXPIRED;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.AppException;

public class ExpiredEstimateDecisionTokenException extends AppException {

    public ExpiredEstimateDecisionTokenException() {
        super("Estimate decision link has expired", DECISION_TOKEN_EXPIRED, GONE);
    }
}
