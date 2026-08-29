package br.com.fiap.postech.soat16.fase1.vehicle.application.command;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.enums.VehicleType;

public record UpdateVehicleCommand(
        UUID id,
        String licensePlate,
        String manufacturer,
        String model,
        String color,
        Integer year,
        Long kmDriven,
        VehicleType type
) { }
