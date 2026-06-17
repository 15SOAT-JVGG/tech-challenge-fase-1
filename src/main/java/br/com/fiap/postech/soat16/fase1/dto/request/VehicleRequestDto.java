package br.com.fiap.postech.soat16.fase1.dto.request;

import java.util.Locale;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import br.com.fiap.postech.soat16.fase1.model.VehicleType;

public record VehicleRequestDto(
    @NotNull(message = "customerId cannot be blank")
    UUID customerId,
    @NotBlank(message = "licensePlate cannot be blank")
    String licensePlate,
    @NotBlank(message = "manufacturer cannot be blank")
    String manufacturer,
    @NotBlank(message = "model cannot be blank")
    String model,
    @NotBlank(message = "color cannot be blank")
    String color,
    @NotNull(message = "year cannot be null")
    Integer year,
    @NotNull(message = "kmDriven cannot be null")
    Long kmDriven,
    @NotNull(message = "type cannot be null")
    VehicleType type
) {

    public VehicleRequestDto {
        if (licensePlate != null) {
            licensePlate = licensePlate.replace("-", "").toUpperCase(Locale.ROOT);
        }
    }
}
