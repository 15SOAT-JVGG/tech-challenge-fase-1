package br.com.fiap.postech.soat16.fase1.workorder.domain.exception;

import static br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorType.VALIDATION;
import static br.com.fiap.postech.soat16.fase1.workorder.domain.exception.EstimateErrorCode.DECISION_TOKEN_INVALID;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.AppException;

/**
 * A mensagem é deliberadamente genérica: distinguir assinatura inválida de token inexistente
 * revelaria a quem tenta adivinhar um link se o orçamento existe.
 */
public class InvalidEstimateDecisionTokenException extends AppException {

    public InvalidEstimateDecisionTokenException() {
        super("Estimate decision link is invalid", DECISION_TOKEN_INVALID, VALIDATION);
    }

    public InvalidEstimateDecisionTokenException(Throwable cause) {
        super("Estimate decision link is invalid", DECISION_TOKEN_INVALID, VALIDATION, cause);
    }
}
