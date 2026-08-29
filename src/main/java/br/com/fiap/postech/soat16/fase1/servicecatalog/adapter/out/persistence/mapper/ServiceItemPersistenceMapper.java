package br.com.fiap.postech.soat16.fase1.servicecatalog.adapter.out.persistence.mapper;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.servicecatalog.adapter.out.persistence.entity.ServiceItemJpaEntity;
import br.com.fiap.postech.soat16.fase1.servicecatalog.domain.model.ServiceItem;
import br.com.fiap.postech.soat16.fase1.shared.adapter.out.persistence.AuditPersistenceMapper;

public final class ServiceItemPersistenceMapper {

    private ServiceItemPersistenceMapper() {
    }

    public static ServiceItem toDomain(ServiceItemJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        var serviceItem = new ServiceItem();
        serviceItem.setId(entity.getId());
        serviceItem.setName(entity.getName());
        serviceItem.setDescription(entity.getDescription());
        serviceItem.setBasePrice(entity.getBasePrice());
        serviceItem.setEstimatedDurationMinutes(entity.getEstimatedDurationMinutes());
        serviceItem.setActive(entity.isActive());
        AuditPersistenceMapper.copyToDomain(entity, serviceItem);
        return serviceItem;
    }

    public static ServiceItemJpaEntity toJpaEntity(ServiceItem serviceItem) {
        if (serviceItem == null) {
            return null;
        }
        var entity = new ServiceItemJpaEntity();
        entity.setId(serviceItem.getId());
        copyState(serviceItem, entity);
        AuditPersistenceMapper.copyToJpaEntity(serviceItem, entity);
        return entity;
    }

    /**
     * Referência somente com identidade, usada para preencher chaves estrangeiras sem carregar o
     * agregado completo de outro contexto.
     */
    public static ServiceItemJpaEntity toJpaReference(UUID serviceItemId) {
        if (serviceItemId == null) {
            return null;
        }
        var entity = new ServiceItemJpaEntity();
        entity.setId(serviceItemId);
        return entity;
    }

    public static void copyState(ServiceItem source, ServiceItemJpaEntity target) {
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setBasePrice(source.getBasePrice());
        target.setEstimatedDurationMinutes(source.getEstimatedDurationMinutes());
        target.setActive(source.isActive());
    }
}
