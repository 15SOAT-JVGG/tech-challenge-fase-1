package br.com.fiap.postech.soat16.fase1.service;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.model.Estimate;
import br.com.fiap.postech.soat16.fase1.model.WorkOrder;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;

/**
 * Canal de notificação ao cliente. Nesta versão (MVP) a entrega é apenas registrada em log,
 * resolvendo a questão em aberto do Event Storming ("envio do orçamento ao cliente — como?")
 * sem acoplar a aplicação a um provedor externo (e-mail/WhatsApp). A troca por um provedor real
 * fica isolada nesta classe.
 */
@ApplicationScoped
public class NotificationService {

    public Uni<Void> notifyEstimateReady(WorkOrder order, Estimate estimate) {
        return Uni.createFrom().voidItem()
                .invoke(() -> Log.infof(
                        "[NOTIFICATION] Orcamento %s disponivel para a OS %s. Total=%s. Aguardando aprovacao do cliente.",
                        estimate.getId(), order.getId(), estimate.getTotalAmount()));
    }

    public Uni<Void> notifyWorkOrderCompleted(WorkOrder order) {
        return Uni.createFrom().voidItem()
                .invoke(() -> Log.infof(
                        "[NOTIFICATION] OS %s finalizada. Cliente notificado para retirada do veiculo.",
                        order.getId()));
    }
}
