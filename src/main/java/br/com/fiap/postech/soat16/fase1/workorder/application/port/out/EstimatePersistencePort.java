package br.com.fiap.postech.soat16.fase1.workorder.application.port.out;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.Estimate;

import io.smallrye.mutiny.Uni;

public interface EstimatePersistencePort {

    Uni<Estimate> findByEstimateIdAndWorkOrderId(UUID estimateId, UUID workOrderId);

    Uni<Estimate> findApprovedByWorkOrderId(UUID workOrderId);

    Uni<Boolean> existsApprovedByWorkOrderId(UUID workOrderId);

    Uni<Estimate> save(Estimate estimate);
}
