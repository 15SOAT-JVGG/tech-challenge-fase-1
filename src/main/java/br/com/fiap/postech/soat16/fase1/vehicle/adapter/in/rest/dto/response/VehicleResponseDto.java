package br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.enums.VehicleType;

public record VehicleResponseDto(
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
) { }
