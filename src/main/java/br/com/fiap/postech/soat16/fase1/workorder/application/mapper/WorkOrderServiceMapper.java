package br.com.fiap.postech.soat16.fase1.workorder.application.mapper;

import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.servicecatalog.domain.model.ServiceItem;
import br.com.fiap.postech.soat16.fase1.workorder.application.command.AddWorkOrderServiceCommand;
import br.com.fiap.postech.soat16.fase1.workorder.application.result.WorkOrderServiceResult;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrder;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrderService;

@ApplicationScoped
public class WorkOrderServiceMapper {

    public WorkOrderServiceResult toResult(WorkOrderService entity) {
        if (entity == null) {
            return null;
        }
        return new WorkOrderServiceResult(
                entity.getId(),
                entity.getWorkOrder().getId(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getPerformedAt(),
                entity.getServiceItem() != null ? entity.getServiceItem().getId() : null
        );
    }

    public WorkOrderService toEntity(AddWorkOrderServiceCommand request,
            WorkOrder workOrder, ServiceItem serviceItem) {
        var entity = new WorkOrderService();
        entity.setWorkOrder(workOrder);
        entity.setDescription(request.description());
        entity.setPrice(request.price());
        entity.setPerformedAt(LocalDateTime.now(ZoneId.systemDefault()));
        entity.setServiceItem(serviceItem);
        return entity;
    }
}
