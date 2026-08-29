package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.entity.EstimateJpaEntity;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.mapper.EstimatePersistenceMapper;
import br.com.fiap.postech.soat16.fase1.workorder.application.port.out.EstimatePersistencePort;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.Estimate;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.EstimateStatus;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class EstimateRepository
        implements PanacheRepositoryBase<EstimateJpaEntity, UUID>, EstimatePersistencePort {

    @Override
    public Uni<Estimate> findByEstimateIdAndWorkOrderId(UUID estimateId, UUID workOrderId) {
        return find("FROM Estimate e LEFT JOIN FETCH e.items i LEFT JOIN FETCH i.part "
                        + "WHERE e.id = ?1 AND e.workOrder.id = ?2", estimateId, workOrderId)
                .firstResult()
                .invoke(found -> Log.infof("Estimate lookup: id=%s workOrderId=%s found=%b",
                        estimateId, workOrderId, found != null))
                .map(EstimatePersistenceMapper::toDomain);
    }

    @Override
    public Uni<Estimate> findApprovedByWorkOrderId(UUID workOrderId) {
        return find("FROM Estimate e LEFT JOIN FETCH e.items i LEFT JOIN FETCH i.part "
                        + "WHERE e.workOrder.id = ?1 AND e.status = ?2", workOrderId, EstimateStatus.APPROVED)
                .firstResult()
                .map(EstimatePersistenceMapper::toDomain);
    }

    @Override
    public Uni<Boolean> existsApprovedByWorkOrderId(UUID workOrderId) {
        return count("workOrder.id = ?1 and status = ?2", workOrderId, EstimateStatus.APPROVED)
                .map(total -> total > 0);
    }

    @Override
    public Uni<Estimate> save(Estimate estimate) {
        return upsert(estimate).replaceWith(estimate);
    }

    private Uni<EstimateJpaEntity> upsert(Estimate estimate) {
        if (estimate.getId() == null) {
            return Panache.getSession()
                    .map(session -> EstimatePersistenceMapper.toJpaEntity(estimate, session))
                    .flatMap(entity -> persist(entity)
                            .invoke(saved -> EstimatePersistenceMapper
                                    .copyGeneratedState(saved, estimate)));
        }
        return findById(estimate.getId())
                .onItem().ifNotNull()
                .invoke(entity -> EstimatePersistenceMapper.copyState(estimate, entity));
    }
}
