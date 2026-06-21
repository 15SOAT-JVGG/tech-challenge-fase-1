package br.com.fiap.postech.soat16.fase1.repository;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.model.WorkOrderService;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class WorkOrderServiceRepository implements PanacheRepository<WorkOrderService> {

    public Uni<List<WorkOrderService>> findByWorkOrderId(UUID workOrderId) {
        return list("workOrder.id = ?1", workOrderId);
    }
}
