package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.mapper;

import br.com.fiap.postech.soat16.fase1.servicecatalog.adapter.out.persistence.mapper.ServiceItemPersistenceMapper;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.entity.WorkOrderServiceJpaEntity;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrderService;

public final class WorkOrderServicePersistenceMapper {

    private WorkOrderServicePersistenceMapper() {
    }

    public static WorkOrderService toDomain(WorkOrderServiceJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        var service = new WorkOrderService();
        service.setId(entity.getId());
        service.setWorkOrder(WorkOrderPersistenceMapper.toDomain(entity.getWorkOrder()));
        service.setServiceItem(ServiceItemPersistenceMapper.toDomain(entity.getServiceItem()));
        service.setDescription(entity.getDescription());
        service.setPrice(entity.getPrice());
        service.setPerformedAt(entity.getPerformedAt());
        return service;
    }

    public static WorkOrderServiceJpaEntity toJpaEntity(WorkOrderService service) {
        if (service == null) {
            return null;
        }
        var entity = new WorkOrderServiceJpaEntity();
        entity.setId(service.getId());
        entity.setWorkOrder(WorkOrderPersistenceMapper.toJpaReference(
                service.getWorkOrder() != null ? service.getWorkOrder().getId() : null));
        entity.setServiceItem(ServiceItemPersistenceMapper.toJpaReference(
                service.getServiceItem() != null ? service.getServiceItem().getId() : null));
        copyState(service, entity);
        return entity;
    }

    public static void copyState(WorkOrderService source, WorkOrderServiceJpaEntity target) {
        target.setDescription(source.getDescription());
        target.setPrice(source.getPrice());
        target.setPerformedAt(source.getPerformedAt());
    }

    public static void copyGeneratedState(WorkOrderServiceJpaEntity source, WorkOrderService target) {
        target.setId(source.getId());
    }
}
