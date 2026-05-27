package br.com.fiap.postech.soat16.fase1.exception;

import static br.com.fiap.postech.soat16.fase1.exception.CustomerErrorCode.PHONE_ALREADY_EXISTS;
import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.CONFLICT;

public class DuplicatePhoneNumberException extends AppException {

    public DuplicatePhoneNumberException() {
        super("This phone number is already in use", PHONE_ALREADY_EXISTS, CONFLICT);
    }
}
