package br.com.fiap.postech.soat16.fase1.exception;

import static br.com.fiap.postech.soat16.fase1.exception.CustomerErrorCode.DOCUMENT_ALREADY_EXISTS;
import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.CONFLICT;

public class DuplicateDocumentException extends AppException {

    public DuplicateDocumentException() {
        super("Este documento (CPF/CNPJ) já está cadastrado", DOCUMENT_ALREADY_EXISTS, CONFLICT);
    }
}
