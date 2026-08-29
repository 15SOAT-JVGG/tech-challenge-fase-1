package br.com.fiap.postech.soat16.fase1.workorder.application.port.out;

import java.util.List;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrderService;

import io.smallrye.mutiny.Uni;

public interface WorkOrderServicePersistencePort {

    Uni<List<WorkOrderService>> findByWorkOrderId(UUID workOrderId);

    Uni<WorkOrderService> save(WorkOrderService service);
}
