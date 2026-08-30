package br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderPriority;

public record WorkOrderRequestDto(
    @NotNull(message = "customerId cannot be blank")
    UUID customerId,
    @NotNull(message = "vehicleId cannot be blank")
    UUID vehicleId,
    @NotBlank(message = "description cannot be blank")
    String description,
    WorkOrderPriority priority,
    UUID assignedWorkerId,
    @Valid List<InitialServiceRequestDto> services,
    @Valid List<InitialPartRequestDto> parts
) {
    public WorkOrderRequestDto {
        services = services == null ? List.of() : List.copyOf(services);
        parts = parts == null ? List.of() : List.copyOf(parts);
    }
}
