package br.com.fiap.postech.soat16.fase1.workorder.application.mapper;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.workorder.application.result.WorkOrderResult;
import br.com.fiap.postech.soat16.fase1.workorder.application.result.WorkOrderTrackingResult;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrder;

@ApplicationScoped
public class WorkOrderMapper {

    public WorkOrderResult toResult(WorkOrder entity) {
        if (entity == null) {
            return null;
        }
        return new WorkOrderResult(
                entity.getId(),
                entity.getCustomer().getId(),
                entity.getVehicle().getId(),
                entity.getDescription(),
                entity.getPriority(),
                entity.getStatus(),
                entity.getOpenedAt(),
                entity.getClosedAt(),
                entity.getCancelledAt(),
                entity.getEstimatedValue(),
                entity.getFinalValue(),
                entity.getAssignedWorker() != null ? entity.getAssignedWorker().getId() : null
        );
    }

    public WorkOrderTrackingResult toTrackingResult(WorkOrder entity) {
        if (entity == null) {
            return null;
        }
        return new WorkOrderTrackingResult(
                entity.getId(),
                entity.getStatus(),
                entity.getOpenedAt(),
                entity.getClosedAt(),
                entity.getCancelledAt()
        );
    }

}
