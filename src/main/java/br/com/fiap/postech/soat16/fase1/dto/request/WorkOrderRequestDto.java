package br.com.fiap.postech.soat16.fase1.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import br.com.fiap.postech.soat16.fase1.model.WorkOrderPriority;

public record WorkOrderRequestDto(
    @NotNull(message = "customerId cannot be blank")
    UUID customerId,
    @NotNull(message = "vehicleId cannot be blank")
    UUID vehicleId,
    @NotBlank(message = "description cannot be blank")
    String description,
    WorkOrderPriority priority
) { }
