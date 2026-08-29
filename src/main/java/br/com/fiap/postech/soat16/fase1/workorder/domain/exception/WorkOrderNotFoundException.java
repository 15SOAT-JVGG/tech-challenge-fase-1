package br.com.fiap.postech.soat16.fase1.workorder.domain.exception;

import static br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorType.NOT_FOUND;
import static br.com.fiap.postech.soat16.fase1.workorder.domain.exception.WorkOrderErrorCode.WORK_ORDER_NOT_FOUND;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.AppException;

public class WorkOrderNotFoundException extends AppException {

    public WorkOrderNotFoundException() {
        super("Work order not found", WORK_ORDER_NOT_FOUND, NOT_FOUND);
    }

    public WorkOrderNotFoundException(UUID id) {
        super("Work order not found: " + id, WORK_ORDER_NOT_FOUND, NOT_FOUND);
    }
}
