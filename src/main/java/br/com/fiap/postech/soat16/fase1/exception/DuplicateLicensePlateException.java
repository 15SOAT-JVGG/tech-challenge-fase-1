package br.com.fiap.postech.soat16.fase1.exception;

import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.CONFLICT;
import static br.com.fiap.postech.soat16.fase1.exception.VehicleErrorCode.LICENSE_PLATE_ALREADY_EXISTS;

public class DuplicateLicensePlateException extends AppException {

    public DuplicateLicensePlateException() {
        super("This license plate is already exists", LICENSE_PLATE_ALREADY_EXISTS, CONFLICT);
    }
}
