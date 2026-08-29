package br.com.fiap.postech.soat16.fase1.customer.domain.exception;

import static br.com.fiap.postech.soat16.fase1.customer.domain.exception.CustomerErrorCode.INVALID_DOCUMENT;
import static br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorType.VALIDATION;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.AppException;

public class InvalidDocumentException extends AppException {

    public InvalidDocumentException(String document) {
        super("Invalid document " + document, INVALID_DOCUMENT, VALIDATION);
    }
}
