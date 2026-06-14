package br.com.fiap.postech.soat16.fase1.exception;

import static br.com.fiap.postech.soat16.fase1.exception.AttendantErrorCode.INVALID_ATTENDANT_CREDENTIALS;
import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.UNAUTHORIZED;

public class InvalidAttendantCredentialsException extends AppException {

    private static final long serialVersionUID = 1L;

    public InvalidAttendantCredentialsException() {
        super("Invalid attendant credentials", INVALID_ATTENDANT_CREDENTIALS, UNAUTHORIZED);
    }
}
