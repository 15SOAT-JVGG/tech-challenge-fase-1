package br.com.fiap.postech.soat16.fase1.exception;

import static br.com.fiap.postech.soat16.fase1.exception.AttendantErrorCode.ATTENDANT_EMAIL_ALREADY_EXISTS;
import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.CONFLICT;

public class DuplicateAttendantEmailException extends AppException {

    private static final long serialVersionUID = 1L;

    public DuplicateAttendantEmailException() {
        super("This attendant email is already in use", ATTENDANT_EMAIL_ALREADY_EXISTS, CONFLICT);
    }
}
