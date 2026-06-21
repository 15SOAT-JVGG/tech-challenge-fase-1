package br.com.fiap.postech.soat16.fase1.exception;

import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.UNAUTHORIZED;
import static br.com.fiap.postech.soat16.fase1.exception.WorkerErrorCode.INVALID_WORKER_CREDENTIALS;

public class InvalidWorkerCredentialsException extends AppException {

    private static final long serialVersionUID = 1L;

    public InvalidWorkerCredentialsException() {
        super("Invalid worker credentials", INVALID_WORKER_CREDENTIALS, UNAUTHORIZED);
    }
}
