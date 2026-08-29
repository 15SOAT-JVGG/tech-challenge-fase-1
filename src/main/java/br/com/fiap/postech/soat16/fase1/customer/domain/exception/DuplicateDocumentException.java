package br.com.fiap.postech.soat16.fase1.customer.domain.exception;

import static br.com.fiap.postech.soat16.fase1.customer.domain.exception.CustomerErrorCode.DOCUMENT_ALREADY_EXISTS;
import static br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorType.CONFLICT;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.AppException;

public class DuplicateDocumentException extends AppException {

    public DuplicateDocumentException() {
        super("This document (CPF/CNPJ) is already registered", DOCUMENT_ALREADY_EXISTS, CONFLICT);
    }
}
