package br.com.fiap.postech.soat16.fase1.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Tempo medio de execucao das ordens de servico, medido entre a abertura (openedAt) e a finalizacao
 * (closedAt) das OS ja concluidas.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkOrderMetricsResponseDto(
    long completedWorkOrders,
    Double averageExecutionMinutes
) { }
