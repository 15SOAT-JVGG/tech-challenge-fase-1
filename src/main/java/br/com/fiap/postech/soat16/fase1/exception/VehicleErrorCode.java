package br.com.fiap.postech.soat16.fase1.exception;

public enum VehicleErrorCode implements ErrorCode {

    VEHICLE_NOT_FOUND,
    LICENSE_PLATE_ALREADY_EXISTS;

    @Override
    public String getCode() {
        return name();
    }
}
