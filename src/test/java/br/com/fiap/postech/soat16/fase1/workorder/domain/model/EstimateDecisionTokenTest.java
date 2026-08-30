package br.com.fiap.postech.soat16.fase1.workorder.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.EstimateDecisionTokenAlreadyUsedException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.ExpiredEstimateDecisionTokenException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.EstimateDecision;

@DisplayName("EstimateDecisionToken model — Unit Tests")
class EstimateDecisionTokenTest {

    private static final LocalDateTime ISSUED_AT = LocalDateTime.of(2026, 1, 10, 9, 0);
    private static final UUID WORK_ORDER_ID = UUID.randomUUID();
    private static final UUID ESTIMATE_ID = UUID.randomUUID();

    @Nested
    @DisplayName("emissão")
    class Issue {

        @Test
        @DisplayName("nasce pendente, ligado ao orçamento e válido por sete dias")
        void issuesTokenValidForSevenDays() {
            EstimateDecisionToken token = issue(EstimateDecision.APPROVE);

            assertEquals(WORK_ORDER_ID, token.getWorkOrderId());
            assertEquals(ESTIMATE_ID, token.getEstimateId());
            assertEquals(EstimateDecision.APPROVE, token.getDecision());
            assertEquals(ISSUED_AT, token.getIssuedAt());
            assertEquals(ISSUED_AT.plusDays(7), token.getExpiresAt());
            assertFalse(token.isConsumed());
        }

        @Test
        @DisplayName("dá a cada emissão um identificador próprio")
        void issuesDistinctIdentifiers() {
            assertNotEquals(issue(EstimateDecision.APPROVE).getId(), issue(EstimateDecision.REJECT).getId());
        }
    }

    @Nested
    @DisplayName("consumo")
    class Consume {

        @Test
        @DisplayName("marca o instante do consumo dentro do prazo")
        void consumesWithinWindow() {
            EstimateDecisionToken token = issue(EstimateDecision.APPROVE);
            LocalDateTime decidedAt = ISSUED_AT.plusDays(6);

            token.consume(decidedAt);

            assertTrue(token.isConsumed());
            assertEquals(decidedAt, token.getConsumedAt());
        }

        @Test
        @DisplayName("recusa um segundo consumo sem alterar o registro do primeiro")
        void rejectsSecondConsumption() {
            EstimateDecisionToken token = issue(EstimateDecision.REJECT);
            LocalDateTime firstUse = ISSUED_AT.plusHours(1);
            token.consume(firstUse);

            assertThrows(EstimateDecisionTokenAlreadyUsedException.class,
                    () -> token.consume(ISSUED_AT.plusHours(2)));
            assertEquals(firstUse, token.getConsumedAt());
        }

        @Test
        @DisplayName("recusa o consumo depois do prazo de sete dias")
        void rejectsExpiredToken() {
            EstimateDecisionToken token = issue(EstimateDecision.APPROVE);

            assertThrows(ExpiredEstimateDecisionTokenException.class,
                    () -> token.consume(ISSUED_AT.plusDays(7).plusSeconds(1)));
            assertFalse(token.isConsumed());
        }

        @Test
        @DisplayName("recusa o consumo exatamente no instante da expiração")
        void rejectsTokenAtExpiryInstant() {
            EstimateDecisionToken token = issue(EstimateDecision.APPROVE);

            assertThrows(ExpiredEstimateDecisionTokenException.class,
                    () -> token.consume(ISSUED_AT.plusDays(7)));
        }

        @Test
        @DisplayName("avalia o consumo repetido antes do prazo, para não revelar a expiração")
        void reportsReuseBeforeExpiry() {
            EstimateDecisionToken token = issue(EstimateDecision.APPROVE);
            token.consume(ISSUED_AT.plusDays(1));

            assertThrows(EstimateDecisionTokenAlreadyUsedException.class,
                    () -> token.consume(ISSUED_AT.plusDays(30)));
        }
    }

    private EstimateDecisionToken issue(EstimateDecision decision) {
        return EstimateDecisionToken.issue(WORK_ORDER_ID, ESTIMATE_ID, decision, ISSUED_AT);
    }
}
