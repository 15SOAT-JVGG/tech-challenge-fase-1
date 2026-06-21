package br.com.fiap.postech.soat16.fase1.exception;

import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.CONFLICT;
import static br.com.fiap.postech.soat16.fase1.exception.WorkerErrorCode.WORKER_EMAIL_ALREADY_EXISTS;

public class DuplicateWorkerEmailException extends AppException {

    private static final long serialVersionUID = 1L;

    public DuplicateWorkerEmailException() {
        super("This worker email is already in use", WORKER_EMAIL_ALREADY_EXISTS, CONFLICT);
    }
}
