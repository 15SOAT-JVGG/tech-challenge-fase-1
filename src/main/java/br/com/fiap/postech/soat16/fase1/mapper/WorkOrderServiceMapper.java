package br.com.fiap.postech.soat16.fase1.mapper;

import java.time.LocalDateTime;

import org.mapstruct.Mapper;

import br.com.fiap.postech.soat16.fase1.dto.request.WorkOrderServiceRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkOrderServiceResponseDto;
import br.com.fiap.postech.soat16.fase1.model.WorkOrder;
import br.com.fiap.postech.soat16.fase1.model.WorkOrderService;

@Mapper(componentModel = "cdi")
public interface WorkOrderServiceMapper {

    default WorkOrderServiceResponseDto toResponse(WorkOrderService entity) {
        if (entity == null) {
            return null;
        }
        return new WorkOrderServiceResponseDto(
                entity.getId(),
                entity.getWorkOrder().getId(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getPerformedAt()
        );
    }

    default WorkOrderService toEntity(WorkOrderServiceRequestDto request, WorkOrder workOrder) {
        var entity = new WorkOrderService();
        entity.setWorkOrder(workOrder);
        entity.setDescription(request.description());
        entity.setPrice(request.price());
        entity.setPerformedAt(LocalDateTime.now());
        return entity;
    }
}
