package br.com.fiap.postech.soat16.fase1.exception;

import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.NOT_FOUND;
import static br.com.fiap.postech.soat16.fase1.exception.EstimateErrorCode.ESTIMATE_NOT_FOUND;

import java.util.UUID;

public class EstimateNotFoundException extends AppException {

    public EstimateNotFoundException() {
        super("Estimate not found", ESTIMATE_NOT_FOUND, NOT_FOUND);
    }

    public EstimateNotFoundException(UUID id) {
        super("Estimate not found: " + id, ESTIMATE_NOT_FOUND, NOT_FOUND);
    }
}
