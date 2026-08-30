package br.com.fiap.postech.soat16.fase1.workorder.domain.exception;

import static br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorType.BUSINESS;
import static br.com.fiap.postech.soat16.fase1.workorder.domain.exception.WorkOrderErrorCode.WORK_ORDER_LOCKED;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.AppException;

public class WorkOrderLockedException extends AppException {

    public WorkOrderLockedException() {
        super("A ordem de serviço foi entregue ou concluída por recusa do orçamento e não pode ser alterada",
                WORK_ORDER_LOCKED, BUSINESS);
    }
}
