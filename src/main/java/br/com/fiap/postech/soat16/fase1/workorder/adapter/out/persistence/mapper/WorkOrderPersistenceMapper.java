package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.mapper;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.customer.adapter.out.persistence.mapper.CustomerPersistenceMapper;
import br.com.fiap.postech.soat16.fase1.shared.adapter.out.persistence.AuditPersistenceMapper;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.out.persistence.mapper.VehiclePersistenceMapper;
import br.com.fiap.postech.soat16.fase1.worker.adapter.out.persistence.mapper.WorkerPersistenceMapper;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.entity.WorkOrderJpaEntity;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrder;

public final class WorkOrderPersistenceMapper {

    private WorkOrderPersistenceMapper() {
    }

    public static WorkOrder toDomain(WorkOrderJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        var workOrder = new WorkOrder();
        workOrder.setId(entity.getId());
        workOrder.setCustomer(CustomerPersistenceMapper.toDomain(entity.getCustomer()));
        workOrder.setVehicle(VehiclePersistenceMapper.toDomain(entity.getVehicle()));
        workOrder.setAssignedWorker(WorkerPersistenceMapper.toDomain(entity.getAssignedWorker()));
        workOrder.setDescription(entity.getDescription());
        workOrder.setPriority(entity.getPriority());
        workOrder.setStatus(entity.getStatus());
        workOrder.setOpenedAt(entity.getOpenedAt());
        workOrder.setClosedAt(entity.getClosedAt());
        workOrder.setCancelledAt(entity.getCancelledAt());
        workOrder.setEstimatedValue(entity.getEstimatedValue());
        workOrder.setFinalValue(entity.getFinalValue());
        AuditPersistenceMapper.copyToDomain(entity, workOrder);
        return workOrder;
    }

    public static WorkOrderJpaEntity toJpaEntity(WorkOrder workOrder) {
        if (workOrder == null) {
            return null;
        }
        var entity = new WorkOrderJpaEntity();
        entity.setId(workOrder.getId());
        entity.setCustomer(CustomerPersistenceMapper.toJpaReference(
                workOrder.getCustomer() != null ? workOrder.getCustomer().getId() : null));
        entity.setVehicle(VehiclePersistenceMapper.toJpaReference(
                workOrder.getVehicle() != null ? workOrder.getVehicle().getId() : null));
        entity.setAssignedWorker(WorkerPersistenceMapper.toJpaReference(
                workOrder.getAssignedWorker() != null ? workOrder.getAssignedWorker().getId() : null));
        copyState(workOrder, entity);
        AuditPersistenceMapper.copyToJpaEntity(workOrder, entity);
        return entity;
    }

    /**
     * Referência somente com identidade, usada para preencher chaves estrangeiras sem carregar o
     * agregado completo.
     */
    public static WorkOrderJpaEntity toJpaReference(UUID workOrderId) {
        if (workOrderId == null) {
            return null;
        }
        var entity = new WorkOrderJpaEntity();
        entity.setId(workOrderId);
        return entity;
    }

    public static void copyState(WorkOrder source, WorkOrderJpaEntity target) {
        target.setDescription(source.getDescription());
        target.setPriority(source.getPriority());
        target.setStatus(source.getStatus());
        target.setOpenedAt(source.getOpenedAt());
        target.setClosedAt(source.getClosedAt());
        target.setCancelledAt(source.getCancelledAt());
        target.setEstimatedValue(source.getEstimatedValue());
        target.setFinalValue(source.getFinalValue());
    }

    public static void copyGeneratedState(WorkOrderJpaEntity source, WorkOrder target) {
        target.setId(source.getId());
        AuditPersistenceMapper.copyToDomain(source, target);
    }
}
