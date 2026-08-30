package br.com.fiap.postech.soat16.fase1.workorder.domain.exception;

import static br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorType.GONE;
import static br.com.fiap.postech.soat16.fase1.workorder.domain.exception.WorkOrderErrorCode.TRACKING_TOKEN_EXPIRED;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.AppException;

public class ExpiredWorkOrderTrackingTokenException extends AppException {

    public ExpiredWorkOrderTrackingTokenException() {
        super("Work order tracking link has expired", TRACKING_TOKEN_EXPIRED, GONE);
    }
}
