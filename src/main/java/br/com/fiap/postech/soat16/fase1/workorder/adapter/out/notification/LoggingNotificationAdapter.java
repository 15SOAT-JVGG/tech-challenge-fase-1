package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.notification;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.workorder.application.port.out.WorkOrderNotificationPort;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.Estimate;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrder;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class LoggingNotificationAdapter implements WorkOrderNotificationPort {

    @Override
    public Uni<Void> notifyEstimateReady(WorkOrder order, Estimate estimate) {
        return Uni.createFrom().voidItem()
                .invoke(() -> Log.infof(
                        "[NOTIFICATION] Orcamento %s disponivel para a OS %s. Total=%s. Aguardando aprovacao do cliente.",
                        estimate.getId(), order.getId(), estimate.getTotalAmount()));
    }

    @Override
    public Uni<Void> notifyWorkOrderCompleted(WorkOrder order) {
        return Uni.createFrom().voidItem()
                .invoke(() -> Log.infof(
                        "[NOTIFICATION] OS %s finalizada. Cliente notificado para retirada do veiculo.",
                        order.getId()));
    }
}
