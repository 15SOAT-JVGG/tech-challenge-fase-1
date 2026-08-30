package br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response;

public record OpenedWorkOrderResponseDto(
        WorkOrderResponseDto workOrder,
        EstimateResponseDto estimate
) { }
