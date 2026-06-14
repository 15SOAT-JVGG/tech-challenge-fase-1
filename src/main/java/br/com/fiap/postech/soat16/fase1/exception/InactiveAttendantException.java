package br.com.fiap.postech.soat16.fase1.exception;

import static br.com.fiap.postech.soat16.fase1.exception.AttendantErrorCode.INACTIVE_ATTENDANT;
import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.UNAUTHORIZED;

public class InactiveAttendantException extends AppException {

    private static final long serialVersionUID = 1L;

    public InactiveAttendantException() {
        super("Attendant is inactive", INACTIVE_ATTENDANT, UNAUTHORIZED);
    }
}
