package br.com.fiap.postech.soat16.fase1.exception;

import static br.com.fiap.postech.soat16.fase1.exception.AttendantErrorCode.ATTENDANT_NOT_FOUND;
import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.NOT_FOUND;

import java.util.UUID;

public class AttendantNotFoundException extends AppException {

    private static final long serialVersionUID = 1L;

    public AttendantNotFoundException(UUID id) {
        super("Attendant not found: " + id, ATTENDANT_NOT_FOUND, NOT_FOUND);
    }
}
