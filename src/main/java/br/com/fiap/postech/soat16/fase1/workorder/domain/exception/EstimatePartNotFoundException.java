package br.com.fiap.postech.soat16.fase1.workorder.domain.exception;

import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.NOT_FOUND;
import static br.com.fiap.postech.soat16.fase1.workorder.domain.exception.EstimateErrorCode.ESTIMATE_PART_NOT_FOUND;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.exception.AppException;

public class EstimatePartNotFoundException extends AppException {

    public EstimatePartNotFoundException(UUID id) {
        super("Part not found: " + id, ESTIMATE_PART_NOT_FOUND, NOT_FOUND);
    }
}
