package br.com.fiap.postech.soat16.fase1.exception;

import static br.com.fiap.postech.soat16.fase1.exception.CustomerErrorCode.DOCUMENT_ALREADY_EXISTS;
import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.CONFLICT;

public class DuplicateDocumentException extends AppException {

    public DuplicateDocumentException() {
        super("This document (CPF/CNPJ) is already registered", DOCUMENT_ALREADY_EXISTS, CONFLICT);
    }
}
