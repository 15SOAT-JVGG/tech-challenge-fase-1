package br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkOrderMetricsResponseDto(
    long completedWorkOrders,
    Double averageExecutionMinutes
) { }
