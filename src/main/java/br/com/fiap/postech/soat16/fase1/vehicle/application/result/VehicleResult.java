package br.com.fiap.postech.soat16.fase1.vehicle.application.result;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.enums.VehicleType;

public record VehicleResult(
        UUID id,
        String licensePlate,
        String manufacturer,
        String model,
        String color,
        Integer year,
        Long kmDriven,
        VehicleType type,
        UUID customerId,
        OffsetDateTime createdAt
) {

    public static VehicleResult from(Vehicle vehicle) {
        return new VehicleResult(
                vehicle.getId(),
                vehicle.getLicensePlate(),
                vehicle.getManufacturer(),
                vehicle.getModel(),
                vehicle.getColor(),
                vehicle.getYear(),
                vehicle.getKmDriven(),
                vehicle.getType(),
                vehicle.getCustomer().getId(),
                vehicle.getCreatedAt());
    }
}
