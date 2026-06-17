package br.com.fiap.postech.soat16.fase1.dto.request;

import br.com.fiap.postech.soat16.fase1.model.VehicleType;

import java.util.UUID;

public record VehicleDto(
    UUID customerId,
    String licensePlate,
    String manufacturer,
    String model,
    String color,
    Integer year,
    Long kmDriven,
    VehicleType type
) {

    public VehicleDto {
        if (licensePlate != null) {
            licensePlate = licensePlate.replace("-", "").toUpperCase();
        }
    }
}
