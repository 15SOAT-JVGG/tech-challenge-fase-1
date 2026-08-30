package br.com.fiap.postech.soat16.fase1.workorder.application.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderPriority;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderStatus;

public record WorkOrderResult(
        UUID workOrderId,
        UUID customerId,
        UUID vehicleId,
        String description,
        WorkOrderPriority priority,
        WorkOrderStatus status,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        LocalDateTime cancelledAt,
        BigDecimal estimatedValue,
        BigDecimal finalValue,
        UUID assignedWorkerId
) { }
