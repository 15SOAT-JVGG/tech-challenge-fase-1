package br.com.fiap.postech.soat16.fase1.dto.request;

import br.com.fiap.postech.soat16.fase1.model.VehicleType;

public record VehicleRequestDto(
    Long id,
    Long customerId,
    String licensePlate,
    String manufacturer,
    String model,
    String color,
    Integer year,
    Long kmDriven,
    VehicleType type
) {

    public VehicleRequestDto {
        if (licensePlate != null) {
            licensePlate = licensePlate.replace("-", "").toUpperCase();
        }
    }
}
