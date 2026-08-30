package br.com.fiap.postech.soat16.fase1.workorder.domain.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.EstimateDecisionTokenAlreadyUsedException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.ExpiredEstimateDecisionTokenException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.EstimateDecision;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * O direito de uma única decisão do cliente sobre um orçamento. A assinatura do link garante que o
 * token veio da oficina; só este registro garante que ele valha uma vez só, porque uma assinatura
 * continua válida por quantas vezes for apresentada.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class EstimateDecisionToken {

    public static final Duration DECISION_WINDOW = Duration.ofDays(7);

    @EqualsAndHashCode.Include
    private UUID id;

    private UUID workOrderId;

    private UUID estimateId;

    private EstimateDecision decision;

    private LocalDateTime issuedAt;

    private LocalDateTime expiresAt;

    private LocalDateTime consumedAt;

    public static EstimateDecisionToken issue(UUID workOrderId, UUID estimateId,
            EstimateDecision decision, LocalDateTime issuedAt) {
        var token = new EstimateDecisionToken();
        token.id = UUID.randomUUID();
        token.workOrderId = workOrderId;
        token.estimateId = estimateId;
        token.decision = decision;
        token.issuedAt = issuedAt;
        token.expiresAt = issuedAt.plus(DECISION_WINDOW);
        return token;
    }

    /**
     * O reuso é verificado antes da expiração: um token já consumido responde sempre a mesma coisa,
     * mesmo depois do prazo, para não expor quando a decisão foi tomada.
     */
    public void consume(LocalDateTime consumedAt) {
        if (isConsumed()) {
            throw new EstimateDecisionTokenAlreadyUsedException();
        }
        if (!consumedAt.isBefore(expiresAt)) {
            throw new ExpiredEstimateDecisionTokenException();
        }
        this.consumedAt = consumedAt;
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }
}
