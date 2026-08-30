package br.com.fiap.postech.soat16.fase1.workorder.domain.exception;

import static br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorType.VALIDATION;
import static br.com.fiap.postech.soat16.fase1.workorder.domain.exception.WorkOrderErrorCode.TRACKING_TOKEN_INVALID;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.AppException;

/**
 * A mensagem é deliberadamente genérica: nada do atendimento é dito a quem apresenta um link que a
 * oficina não emitiu.
 */
public class InvalidWorkOrderTrackingTokenException extends AppException {

    public InvalidWorkOrderTrackingTokenException() {
        super("Work order tracking link is invalid", TRACKING_TOKEN_INVALID, VALIDATION);
    }

    public InvalidWorkOrderTrackingTokenException(Throwable cause) {
        super("Work order tracking link is invalid", TRACKING_TOKEN_INVALID, VALIDATION, cause);
    }
}
