package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.workorder.application.port.out.WorkOrderServicePersistencePort;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrderService;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class WorkOrderServiceRepository
        implements PanacheRepository<WorkOrderService>, WorkOrderServicePersistencePort {

    @Override
    public Uni<List<WorkOrderService>> findByWorkOrderId(UUID workOrderId) {
        return list("workOrder.id = ?1", workOrderId);
    }

    @Override
    public Uni<WorkOrderService> save(WorkOrderService service) {
        return persist(service);
    }
}
