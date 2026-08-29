package br.com.fiap.postech.soat16.fase1.workorder.application.port.out;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.Estimate;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrder;

import io.smallrye.mutiny.Uni;

public interface WorkOrderNotificationPort {

    Uni<Void> notifyEstimateReady(WorkOrder order, Estimate estimate);

    Uni<Void> notifyWorkOrderCompleted(WorkOrder order);
}
