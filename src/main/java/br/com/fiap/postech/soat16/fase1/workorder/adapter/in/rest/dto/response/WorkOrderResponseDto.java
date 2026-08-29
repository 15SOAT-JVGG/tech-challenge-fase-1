package br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderPriority;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkOrderResponseDto(
    UUID workOrderId,
    UUID customerId,
    UUID vehicleId,
    String description,
    WorkOrderPriority priority,
    WorkOrderStatus status,
    LocalDateTime openedAt,
    LocalDateTime closedAt,
    BigDecimal estimatedValue,
    BigDecimal finalValue,
    UUID assignedWorkerId
) { }
