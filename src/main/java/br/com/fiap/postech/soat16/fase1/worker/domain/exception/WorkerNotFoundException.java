package br.com.fiap.postech.soat16.fase1.worker.domain.exception;

import static br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorType.NOT_FOUND;
import static br.com.fiap.postech.soat16.fase1.worker.domain.exception.WorkerErrorCode.WORKER_NOT_FOUND;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.AppException;

public class WorkerNotFoundException extends AppException {

    private static final long serialVersionUID = 1L;

    public WorkerNotFoundException(UUID id) {
        super("Worker not found: " + id, WORKER_NOT_FOUND, NOT_FOUND);
    }
}
