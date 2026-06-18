package br.com.fiap.postech.soat16.fase1.exception;

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
