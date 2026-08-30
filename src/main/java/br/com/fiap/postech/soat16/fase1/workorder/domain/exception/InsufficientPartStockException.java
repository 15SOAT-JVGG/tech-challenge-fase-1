package br.com.fiap.postech.soat16.fase1.workorder.domain.exception;

import static br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorType.BUSINESS;
import static br.com.fiap.postech.soat16.fase1.workorder.domain.exception.EstimateErrorCode.INSUFFICIENT_PART_STOCK;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.AppException;

public class InsufficientPartStockException extends AppException {

    public InsufficientPartStockException(String partName, int requested, int available) {
        super("Insufficient stock to reserve part '" + partName + "'. Requested: " + requested
                + ", available: " + available, INSUFFICIENT_PART_STOCK, BUSINESS);
    }
}
