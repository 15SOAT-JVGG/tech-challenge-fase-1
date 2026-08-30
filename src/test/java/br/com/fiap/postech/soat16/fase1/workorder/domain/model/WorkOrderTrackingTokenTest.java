package br.com.fiap.postech.soat16.fase1.workorder.domain.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.ExpiredWorkOrderTrackingTokenException;

@DisplayName("WorkOrderTrackingToken — Unit Tests")
class WorkOrderTrackingTokenTest {

    private static final LocalDateTime ISSUED_AT = LocalDateTime.of(2026, 1, 10, 9, 0);
    private static final UUID WORK_ORDER_ID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @Nested
    @DisplayName("emissão")
    class Issue {

        @Test
        @DisplayName("nasce ligado à ordem de serviço e válido por trinta dias")
        void issuesTokenValidForThirtyDays() {
            WorkOrderTrackingToken token = WorkOrderTrackingToken.issue(WORK_ORDER_ID, ISSUED_AT);

            assertEquals(WORK_ORDER_ID, token.workOrderId());
            assertEquals(ISSUED_AT, token.issuedAt());
            assertEquals(ISSUED_AT.plusDays(30), token.expiresAt());
        }

        @Test
        @DisplayName("dá o mesmo prazo a cada acompanhamento reemitido para a mesma ordem")
        void issuesTheSameWindowForEveryEmission() {
            WorkOrderTrackingToken first = WorkOrderTrackingToken.issue(WORK_ORDER_ID, ISSUED_AT);
            WorkOrderTrackingToken later = WorkOrderTrackingToken.issue(WORK_ORDER_ID, ISSUED_AT.plusDays(3));

            assertEquals(first.expiresAt().plusDays(3), later.expiresAt());
        }
    }

    @Nested
    @DisplayName("prazo de acompanhamento")
    class TrackingWindow {

        @Test
        @DisplayName("aceita a consulta dentro do prazo")
        void acceptsTrackingWithinTheWindow() {
            WorkOrderTrackingToken token = WorkOrderTrackingToken.issue(WORK_ORDER_ID, ISSUED_AT);

            assertDoesNotThrow(() -> token.ensureValidAt(ISSUED_AT.plusDays(29)));
        }

        @Test
        @DisplayName("recusa a consulta depois dos trinta dias")
        void rejectsTrackingAfterTheWindow() {
            WorkOrderTrackingToken token = WorkOrderTrackingToken.issue(WORK_ORDER_ID, ISSUED_AT);

            assertThrows(ExpiredWorkOrderTrackingTokenException.class,
                    () -> token.ensureValidAt(ISSUED_AT.plusDays(31)));
        }

        @Test
        @DisplayName("recusa a consulta exatamente no instante da expiração")
        void rejectsTrackingAtExpiryInstant() {
            WorkOrderTrackingToken token = WorkOrderTrackingToken.issue(WORK_ORDER_ID, ISSUED_AT);

            assertThrows(ExpiredWorkOrderTrackingTokenException.class,
                    () -> token.ensureValidAt(token.expiresAt()));
        }
    }
}
