package br.com.fiap.postech.soat16.fase1.exception;

import static br.com.fiap.postech.soat16.fase1.exception.CustomerErrorCode.CUSTOMER_NOT_FOUND;
import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.NOT_FOUND;

import java.util.UUID;

public class CustomerNotFoundException extends AppException {

    public CustomerNotFoundException(UUID id) {
        super("Customer not found: " + id, CUSTOMER_NOT_FOUND, NOT_FOUND);
    }

    public CustomerNotFoundException(String message) {
        super(message, CUSTOMER_NOT_FOUND, NOT_FOUND);
    }
}
