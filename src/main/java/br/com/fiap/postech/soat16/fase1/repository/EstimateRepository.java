package br.com.fiap.postech.soat16.fase1.repository;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.model.Estimate;
import br.com.fiap.postech.soat16.fase1.model.EstimateStatus;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class EstimateRepository implements PanacheRepository<Estimate> {

    public Uni<Estimate> findByEstimateIdAndWorkOrderId(UUID estimateId, UUID workOrderId) {
        return find("id = ?1 and workOrder.id = ?2", estimateId, workOrderId).firstResult()
                .invoke(found -> Log.infof("Estimate lookup: id=%s workOrderId=%s found=%b",
                        estimateId, workOrderId, found != null));
    }

    public Uni<Boolean> existsApprovedByWorkOrderId(UUID workOrderId) {
        return count("workOrder.id = ?1 and status = ?2", workOrderId, EstimateStatus.APPROVED)
                .map(total -> total > 0);
    }
}
