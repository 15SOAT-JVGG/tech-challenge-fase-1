package br.com.fiap.postech.soat16.fase1.exception;

import static br.com.fiap.postech.soat16.fase1.exception.WorkerErrorCode.INACTIVE_WORKER;
import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.UNAUTHORIZED;

public class InactiveWorkerException extends AppException {

    private static final long serialVersionUID = 1L;

    public InactiveWorkerException() {
        super("Worker is inactive", INACTIVE_WORKER, UNAUTHORIZED);
    }
}
