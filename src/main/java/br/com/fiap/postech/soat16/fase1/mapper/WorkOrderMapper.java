package br.com.fiap.postech.soat16.fase1.mapper;

import java.time.LocalDateTime;

import org.mapstruct.Mapper;

import br.com.fiap.postech.soat16.fase1.dto.request.WorkOrderRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkOrderResponseDto;
import br.com.fiap.postech.soat16.fase1.model.Customer;
import br.com.fiap.postech.soat16.fase1.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.model.WorkOrder;
import br.com.fiap.postech.soat16.fase1.model.WorkOrderPriority;
import br.com.fiap.postech.soat16.fase1.model.WorkOrderStatus;

@Mapper(componentModel = "cdi")
public interface WorkOrderMapper {

    default WorkOrderResponseDto toResponse(WorkOrder entity) {
        if (entity == null) {
            return null;
        }
        return new WorkOrderResponseDto(
                entity.getId(),
                entity.getCustomer().getId(),
                entity.getVehicle().getId(),
                entity.getDescription(),
                entity.getPriority(),
                entity.getStatus(),
                entity.getOpenedAt(),
                entity.getClosedAt(),
                entity.getEstimatedValue(),
                entity.getFinalValue()
        );
    }

    default WorkOrder toEntity(WorkOrderRequestDto request, Customer customer, Vehicle vehicle) {
        var entity = new WorkOrder();
        entity.setCustomer(customer);
        entity.setVehicle(vehicle);
        entity.setDescription(request.description());
        entity.setPriority(request.priority() != null ? request.priority() : WorkOrderPriority.MEDIUM);
        entity.setStatus(WorkOrderStatus.OPEN);
        entity.setOpenedAt(LocalDateTime.now());
        return entity;
    }
}
