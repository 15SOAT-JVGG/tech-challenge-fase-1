package br.com.fiap.postech.soat16.fase1.worker.domain.exception;

import static br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorType.UNAUTHORIZED;
import static br.com.fiap.postech.soat16.fase1.worker.domain.exception.WorkerErrorCode.INACTIVE_WORKER;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.AppException;

public class InactiveWorkerException extends AppException {

    private static final long serialVersionUID = 1L;

    public InactiveWorkerException() {
        super("Worker is inactive", INACTIVE_WORKER, UNAUTHORIZED);
    }
}
