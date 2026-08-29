package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.entity.WorkOrderHistoryJpaEntity;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.mapper.WorkOrderHistoryPersistenceMapper;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrderHistory;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class WorkOrderHistoryRepository
        implements PanacheRepositoryBase<WorkOrderHistoryJpaEntity, UUID> {

    public Uni<WorkOrderHistory> save(WorkOrderHistory history) {
        return persist(WorkOrderHistoryPersistenceMapper.toJpaEntity(history))
                .invoke(entity -> WorkOrderHistoryPersistenceMapper.copyGeneratedState(entity, history))
                .replaceWith(history);
    }
}
