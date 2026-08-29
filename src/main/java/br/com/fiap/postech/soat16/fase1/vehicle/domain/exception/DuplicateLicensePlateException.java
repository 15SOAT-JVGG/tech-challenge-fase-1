package br.com.fiap.postech.soat16.fase1.vehicle.domain.exception;

import static br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorType.CONFLICT;
import static br.com.fiap.postech.soat16.fase1.vehicle.domain.exception.VehicleErrorCode.LICENSE_PLATE_ALREADY_EXISTS;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.AppException;

public class DuplicateLicensePlateException extends AppException {

    public DuplicateLicensePlateException() {
        super("This license plate is already exists", LICENSE_PLATE_ALREADY_EXISTS, CONFLICT);
    }
}
