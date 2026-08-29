package br.com.fiap.postech.soat16.fase1.workorder.domain.exception;

import static br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorType.NOT_FOUND;
import static br.com.fiap.postech.soat16.fase1.workorder.domain.exception.EstimateErrorCode.ESTIMATE_NOT_FOUND;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.AppException;

public class EstimateNotFoundException extends AppException {

    public EstimateNotFoundException() {
        super("Estimate not found", ESTIMATE_NOT_FOUND, NOT_FOUND);
    }

    public EstimateNotFoundException(UUID id) {
        super("Estimate not found: " + id, ESTIMATE_NOT_FOUND, NOT_FOUND);
    }
}
