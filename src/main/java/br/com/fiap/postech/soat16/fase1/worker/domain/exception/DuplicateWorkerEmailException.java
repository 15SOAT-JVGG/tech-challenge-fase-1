package br.com.fiap.postech.soat16.fase1.worker.domain.exception;

import static br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorType.CONFLICT;
import static br.com.fiap.postech.soat16.fase1.worker.domain.exception.WorkerErrorCode.WORKER_EMAIL_ALREADY_EXISTS;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.AppException;

public class DuplicateWorkerEmailException extends AppException {

    private static final long serialVersionUID = 1L;

    public DuplicateWorkerEmailException() {
        super("This worker email is already in use", WORKER_EMAIL_ALREADY_EXISTS, CONFLICT);
    }
}
