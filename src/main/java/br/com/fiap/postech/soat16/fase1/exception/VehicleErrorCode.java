package br.com.fiap.postech.soat16.fase1.exception;

public enum VehicleErrorCode implements ErrorCode {

    LICENSE_PLATE_ALREADY_EXISTS;

    @Override
    public String getCode() {
        return "";
    }
}
