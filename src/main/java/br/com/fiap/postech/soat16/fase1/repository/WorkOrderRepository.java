package br.com.fiap.postech.soat16.fase1.repository;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.model.WorkOrder;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class WorkOrderRepository implements PanacheRepository<WorkOrder> {

    private static final String ORDER_BY_PRIORITY =
            "ORDER BY CASE priority "
            + "WHEN br.com.fiap.postech.soat16.fase1.model.WorkOrderPriority.URGENT THEN 0 "
            + "WHEN br.com.fiap.postech.soat16.fase1.model.WorkOrderPriority.HIGH THEN 1 "
            + "WHEN br.com.fiap.postech.soat16.fase1.model.WorkOrderPriority.MEDIUM THEN 2 "
            + "WHEN br.com.fiap.postech.soat16.fase1.model.WorkOrderPriority.LOW THEN 3 "
            + "ELSE 4 END, createdAt DESC";

    public Uni<List<WorkOrder>> findPage(int page, int size) {
        return find(ORDER_BY_PRIORITY).page(page, size).list();
    }

    public Uni<WorkOrder> findByWorkOrderId(UUID id) {
        return find("id = ?1", id).firstResult()
                .invoke(found -> Log.infof("WorkOrder lookup: id=%s found=%b", id, found != null));
    }

    public Uni<List<WorkOrder>> findClosed() {
        return list("openedAt is not null and closedAt is not null");
    }
}
