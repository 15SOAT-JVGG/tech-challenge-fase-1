package br.com.fiap.postech.soat16.fase1.workorder.application.command;

import java.util.List;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderPriority;

public record OpenWorkOrderCommand(
        UUID customerId,
        UUID vehicleId,
        String description,
        WorkOrderPriority priority,
        UUID assignedWorkerId,
        List<RequestedService> services,
        List<RequestedPart> parts
) {

    public OpenWorkOrderCommand {
        services = services == null ? List.of() : List.copyOf(services);
        parts = parts == null ? List.of() : List.copyOf(parts);
    }

    public boolean hasInitialRequest() {
        return !services.isEmpty() || !parts.isEmpty();
    }

    public record RequestedService(UUID serviceItemId) { }

    public record RequestedPart(UUID partId, int quantity) { }
}
