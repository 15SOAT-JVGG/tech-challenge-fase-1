package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.workorder.application.port.out.WorkOrderPersistencePort;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrder;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrderHistory;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class WorkOrderRepository implements PanacheRepository<WorkOrder>, WorkOrderPersistencePort {

    private static final String ORDER_BY_PRIORITY =
            "ORDER BY CASE priority "
            + "WHEN br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderPriority.URGENT THEN 0 "
            + "WHEN br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderPriority.HIGH THEN 1 "
            + "WHEN br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderPriority.MEDIUM THEN 2 "
            + "WHEN br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderPriority.LOW THEN 3 "
            + "ELSE 4 END, createdAt DESC";

    private final WorkOrderHistoryRepository historyRepository;

    @Override
    public Uni<List<WorkOrder>> findPage(int page, int size) {
        return find(ORDER_BY_PRIORITY).page(page, size).list();
    }

    @Override
    public Uni<Long> countWorkOrders() {
        return count();
    }

    @Override
    public Uni<WorkOrder> findByWorkOrderId(UUID id) {
        return find("id = ?1", id).firstResult()
                .invoke(found -> Log.infof("WorkOrder lookup: id=%s found=%b", id, found != null));
    }

    @Override
    public Uni<List<WorkOrder>> findClosed() {
        return list("openedAt is not null and closedAt is not null");
    }

    @Override
    public Uni<WorkOrder> save(WorkOrder workOrder) {
        return persist(workOrder);
    }

    @Override
    public Uni<WorkOrder> saveWithHistory(WorkOrder workOrder, WorkOrderHistory history) {
        return historyRepository.persist(history)
                .flatMap(ignored -> persist(workOrder));
    }
}
