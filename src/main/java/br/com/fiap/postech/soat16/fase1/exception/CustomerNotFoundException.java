package br.com.fiap.postech.soat16.fase1.exception;

import static br.com.fiap.postech.soat16.fase1.exception.CustomerErrorCode.CUSTOMER_NOT_FOUND;
import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.NOT_FOUND;

import java.util.UUID;

public class CustomerNotFoundException extends AppException {

    public CustomerNotFoundException() {
        super("Customer not found", CUSTOMER_NOT_FOUND, NOT_FOUND);
    }

    public CustomerNotFoundException(String document) {
        super("Customer not found for document: " + document, CUSTOMER_NOT_FOUND, NOT_FOUND);
    }
}
