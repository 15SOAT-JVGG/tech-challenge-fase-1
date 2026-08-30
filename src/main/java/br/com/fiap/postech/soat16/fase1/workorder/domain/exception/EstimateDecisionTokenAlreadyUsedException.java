package br.com.fiap.postech.soat16.fase1.workorder.domain.exception;

import static br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorType.GONE;
import static br.com.fiap.postech.soat16.fase1.workorder.domain.exception.EstimateErrorCode.DECISION_TOKEN_ALREADY_USED;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.AppException;

public class EstimateDecisionTokenAlreadyUsedException extends AppException {

    public EstimateDecisionTokenAlreadyUsedException() {
        super("Estimate decision link has already been used", DECISION_TOKEN_ALREADY_USED, GONE);
    }
}
