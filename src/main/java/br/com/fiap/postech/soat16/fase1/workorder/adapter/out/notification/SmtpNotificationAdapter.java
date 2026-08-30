package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.notification;

import java.time.format.DateTimeFormatter;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import br.com.fiap.postech.soat16.fase1.workorder.application.port.out.EstimateDecisionInvitation;
import br.com.fiap.postech.soat16.fase1.workorder.application.port.out.WorkOrderNotificationPort;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.Estimate;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrder;

import io.quarkus.logging.Log;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;

/**
 * Entrega por SMTP as mensagens que a oficina deve ao cliente. É este adaptador que conhece o
 * endereço público da API e monta a URL de cada link de decisão a partir do token assinado.
 *
 * <p>Uma OS sem e-mail de cliente cadastrado não interrompe o atendimento: a falta é registrada em
 * log e o fluxo segue, porque a reserva de estoque e a mudança de status já foram decididas.
 */
@ApplicationScoped
public class SmtpNotificationAdapter implements WorkOrderNotificationPort {

    private static final String DECISION_PATH = "/v1/public/work-orders/estimate-decisions/";
    private static final DateTimeFormatter DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final ReactiveMailer mailer;
    private final String publicBaseUrl;

    public SmtpNotificationAdapter(
            ReactiveMailer mailer,
            @ConfigProperty(name = "app.public-base-url", defaultValue = "http://localhost:8080")
            String publicBaseUrl) {
        this.mailer = mailer;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public Uni<Void> notifyEstimateAwaitingDecision(WorkOrder order, Estimate estimate,
            EstimateDecisionInvitation invitation) {
        String recipient = customerEmail(order);
        if (recipient == null) {
            return skip(order, "orcamento aguardando decisao");
        }
        String body = """
                Ola, %s!

                O orcamento da sua ordem de servico %s esta pronto: R$ %s.

                Aprovar o servico:  %s
                Recusar o servico:  %s

                Os links valem para uma unica resposta e expiram em %s.
                """.formatted(
                        customerName(order),
                        order.getId(),
                        estimate.getTotalAmount(),
                        decisionLink(invitation.approveToken()),
                        decisionLink(invitation.rejectToken()),
                        invitation.expiresAt().format(DEADLINE_FORMAT));
        return mailer.send(Mail.withText(recipient,
                "Orcamento da ordem de servico " + order.getId(), body));
    }

    @Override
    public Uni<Void> notifyWorkOrderCompleted(WorkOrder order) {
        String recipient = customerEmail(order);
        if (recipient == null) {
            return skip(order, "ordem de servico concluida");
        }
        String body = """
                Ola, %s!

                A ordem de servico %s foi concluida e o veiculo esta disponivel para retirada.
                """.formatted(customerName(order), order.getId());
        return mailer.send(Mail.withText(recipient,
                "Ordem de servico " + order.getId() + " concluida", body));
    }

    private Uni<Void> skip(WorkOrder order, String subject) {
        Log.warnf("Notificacao nao enviada: OS %s nao tem e-mail de cliente. Assunto=%s",
                order.getId(), subject);
        return Uni.createFrom().voidItem();
    }

    private String decisionLink(String signedToken) {
        return publicBaseUrl + DECISION_PATH + signedToken;
    }

    private String customerEmail(WorkOrder order) {
        return order.getCustomer() != null ? order.getCustomer().getEmail() : null;
    }

    private String customerName(WorkOrder order) {
        return order.getCustomer() != null ? order.getCustomer().getFirstName() : "cliente";
    }
}
