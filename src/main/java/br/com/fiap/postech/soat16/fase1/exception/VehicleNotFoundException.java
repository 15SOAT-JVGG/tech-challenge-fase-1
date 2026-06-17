package br.com.fiap.postech.soat16.fase1.exception;

import static br.com.fiap.postech.soat16.fase1.exception.ErrorType.NOT_FOUND;
import static br.com.fiap.postech.soat16.fase1.exception.VehicleErrorCode.VEHICLE_NOT_FOUND;


public class VehicleNotFoundException extends AppException {

    public VehicleNotFoundException() {
        super("Vehicle not found", VEHICLE_NOT_FOUND, NOT_FOUND);
    }

    public VehicleNotFoundException(String licensePlate) {
        super("Vehicle not found for license plate: " + licensePlate, VEHICLE_NOT_FOUND, NOT_FOUND);
    }
}
