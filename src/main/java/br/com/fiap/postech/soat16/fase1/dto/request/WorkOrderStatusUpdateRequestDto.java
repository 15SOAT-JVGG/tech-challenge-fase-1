package br.com.fiap.postech.soat16.fase1.dto.request;

import jakarta.validation.constraints.NotNull;

import br.com.fiap.postech.soat16.fase1.model.enums.WorkOrderStatus;

public record WorkOrderStatusUpdateRequestDto(
    @NotNull(message = "status cannot be null")
    WorkOrderStatus status
) { }
