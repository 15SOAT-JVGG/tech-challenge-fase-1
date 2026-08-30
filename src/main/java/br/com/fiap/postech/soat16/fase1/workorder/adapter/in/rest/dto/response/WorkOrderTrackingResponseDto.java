package br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkOrderTrackingResponseDto(
    UUID workOrderId,
    WorkOrderStatus status,
    LocalDateTime openedAt,
    LocalDateTime closedAt,
    LocalDateTime cancelledAt
) { }
