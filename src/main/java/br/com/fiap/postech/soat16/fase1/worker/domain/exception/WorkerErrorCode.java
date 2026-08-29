package br.com.fiap.postech.soat16.fase1.worker.domain.exception;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorCode;

public enum WorkerErrorCode implements ErrorCode {

    WORKER_NOT_FOUND,
    WORKER_EMAIL_ALREADY_EXISTS,
    INVALID_WORKER_CREDENTIALS,
    INACTIVE_WORKER;

    @Override
    public String getCode() {
        return name();
    }
}
