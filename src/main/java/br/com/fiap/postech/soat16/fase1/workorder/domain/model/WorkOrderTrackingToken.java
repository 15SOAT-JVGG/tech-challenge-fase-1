package br.com.fiap.postech.soat16.fase1.workorder.domain.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.ExpiredWorkOrderTrackingTokenException;

/**
 * O direito de acompanhar uma ordem de serviço pelo link que a oficina enviou. Ao contrário do
 * {@link EstimateDecisionToken}, acompanhar não decide nada e não se gasta: o mesmo link vale por
 * trinta dias e quantas vezes o cliente quiser abri-lo. Por isso não há registro no banco — a data
 * de emissão que viaja assinada no link é tudo o que o prazo precisa.
 */
public record WorkOrderTrackingToken(UUID workOrderId, LocalDateTime issuedAt, LocalDateTime expiresAt) {

    public static final Duration TRACKING_WINDOW = Duration.ofDays(30);

    public static WorkOrderTrackingToken issue(UUID workOrderId, LocalDateTime issuedAt) {
        return new WorkOrderTrackingToken(workOrderId, issuedAt, issuedAt.plus(TRACKING_WINDOW));
    }

    public void ensureValidAt(LocalDateTime moment) {
        if (!moment.isBefore(expiresAt)) {
            throw new ExpiredWorkOrderTrackingTokenException();
        }
    }
}
