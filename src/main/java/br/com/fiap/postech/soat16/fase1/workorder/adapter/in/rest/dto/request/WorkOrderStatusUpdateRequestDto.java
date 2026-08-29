package br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotNull;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderStatus;

public record WorkOrderStatusUpdateRequestDto(
    @NotNull(message = "status cannot be null")
    WorkOrderStatus status
) { }
