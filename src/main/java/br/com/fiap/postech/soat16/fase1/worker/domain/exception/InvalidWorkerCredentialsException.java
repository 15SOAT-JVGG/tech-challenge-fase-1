package br.com.fiap.postech.soat16.fase1.worker.domain.exception;

import static br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorType.UNAUTHORIZED;
import static br.com.fiap.postech.soat16.fase1.worker.domain.exception.WorkerErrorCode.INVALID_WORKER_CREDENTIALS;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.AppException;

public class InvalidWorkerCredentialsException extends AppException {

    private static final long serialVersionUID = 1L;

    public InvalidWorkerCredentialsException() {
        super("Invalid worker credentials", INVALID_WORKER_CREDENTIALS, UNAUTHORIZED);
    }
}
