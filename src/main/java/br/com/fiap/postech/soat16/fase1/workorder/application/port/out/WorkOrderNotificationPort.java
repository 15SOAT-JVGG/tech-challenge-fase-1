package br.com.fiap.postech.soat16.fase1.workorder.application.port.out;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.Estimate;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrder;

import io.smallrye.mutiny.Uni;

public interface WorkOrderNotificationPort {

    Uni<Void> notifyEstimateAwaitingDecision(WorkOrder order, Estimate estimate,
            EstimateDecisionInvitation invitation);

    Uni<Void> notifyWorkOrderCompleted(WorkOrder order);
}
