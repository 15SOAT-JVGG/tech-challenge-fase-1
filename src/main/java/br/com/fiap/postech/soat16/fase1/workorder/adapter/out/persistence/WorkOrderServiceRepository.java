package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.entity.WorkOrderServiceJpaEntity;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.mapper.WorkOrderServicePersistenceMapper;
import br.com.fiap.postech.soat16.fase1.workorder.application.port.out.WorkOrderServicePersistencePort;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrderService;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class WorkOrderServiceRepository
        implements PanacheRepositoryBase<WorkOrderServiceJpaEntity, UUID>,
        WorkOrderServicePersistencePort {

    @Override
    public Uni<List<WorkOrderService>> findByWorkOrderId(UUID workOrderId) {
        return list("workOrder.id = ?1", workOrderId)
                .map(entities -> entities.stream()
                        .map(WorkOrderServicePersistenceMapper::toDomain)
                        .toList());
    }

    @Override
    public Uni<WorkOrderService> save(WorkOrderService service) {
        return upsert(service).replaceWith(service);
    }

    private Uni<WorkOrderServiceJpaEntity> upsert(WorkOrderService service) {
        if (service.getId() == null) {
            return persist(WorkOrderServicePersistenceMapper.toJpaEntity(service))
                    .invoke(entity -> WorkOrderServicePersistenceMapper
                            .copyGeneratedState(entity, service));
        }
        return findById(service.getId())
                .onItem().ifNotNull()
                .invoke(entity -> WorkOrderServicePersistenceMapper.copyState(service, entity));
    }
}
