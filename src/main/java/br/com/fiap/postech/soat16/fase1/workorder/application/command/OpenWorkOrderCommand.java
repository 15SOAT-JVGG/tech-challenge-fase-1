package br.com.fiap.postech.soat16.fase1.workorder.application.command;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderPriority;

public record OpenWorkOrderCommand(
        UUID customerId,
        UUID vehicleId,
        String description,
        WorkOrderPriority priority,
        UUID assignedWorkerId
) { }
