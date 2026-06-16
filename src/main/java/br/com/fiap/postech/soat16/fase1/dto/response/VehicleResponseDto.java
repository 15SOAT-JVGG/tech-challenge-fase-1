package br.com.fiap.postech.soat16.fase1.dto.response;

import br.com.fiap.postech.soat16.fase1.model.VehicleType;

import java.util.UUID;

public record VehicleResponseDto(
        UUID id,
        String licensePlate,
        String manufacturer,
        String model,
        String color,
        Integer year,
        Long kmDriven,
        VehicleType type,
        Long customerId
) { }
