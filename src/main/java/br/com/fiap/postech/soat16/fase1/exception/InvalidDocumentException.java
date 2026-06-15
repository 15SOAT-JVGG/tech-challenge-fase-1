package br.com.fiap.postech.soat16.fase1.exception;

import static br.com.fiap.postech.soat16.fase1.exception.CustomerErrorCode.INVALID_DOCUMENT;
import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.VALIDATION;

public class InvalidDocumentException extends AppException {

    public InvalidDocumentException(String document) {
        super("Documento inválido: " + document, INVALID_DOCUMENT, VALIDATION);
    }
}
