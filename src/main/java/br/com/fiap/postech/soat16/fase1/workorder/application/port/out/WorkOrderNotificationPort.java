package br.com.fiap.postech.soat16.fase1.workorder.application.port.out;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.Estimate;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrder;

import io.smallrye.mutiny.Uni;

public interface WorkOrderNotificationPort {

    Uni<Void> notifyEstimateAwaitingDecision(WorkOrder order, Estimate estimate,
            EstimateDecisionInvitation invitation);

    /**
     * Avisa o cliente do estágio em que o atendimento está e devolve o link de acompanhamento.
     * Vale para a abertura e para cada mudança de status posterior, inclusive a conclusão.
     */
    Uni<Void> notifyWorkOrderProgress(WorkOrder order, WorkOrderTrackingInvitation invitation);
}
