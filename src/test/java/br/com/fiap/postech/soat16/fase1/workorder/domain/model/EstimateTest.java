package br.com.fiap.postech.soat16.fase1.workorder.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.part.domain.model.Part;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.EstimateAlreadyDecidedException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.InsufficientPartStockException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.EstimateStatus;

@DisplayName("Estimate model — Unit Tests")
class EstimateTest {

    @Test
    @DisplayName("creates a pending estimate with parts and labor totals")
    void createsEstimateTotals() {
        WorkOrder order = new WorkOrder();
        EstimateItem first = item("25.00", 2);
        EstimateItem second = item("10.00", 3);
        WorkOrderService labor = new WorkOrderService();
        labor.setPrice(new BigDecimal("120.00"));

        Estimate estimate = Estimate.create(order, List.of(first, second), List.of(labor));

        assertEquals(EstimateStatus.PENDING, estimate.getStatus());
        assertEquals(new BigDecimal("80.00"), estimate.getPartsAmount());
        assertEquals(new BigDecimal("120.00"), estimate.getLaborAmount());
        assertEquals(new BigDecimal("200.00"), estimate.getTotalAmount());
        assertEquals(estimate, first.getEstimate());
        assertEquals(estimate, second.getEstimate());
    }

    @Nested
    @DisplayName("decision")
    class Decision {

        @Test
        @DisplayName("approves a pending estimate at the supplied time")
        void approvesPendingEstimate() {
            Estimate estimate = pendingEstimate();
            LocalDateTime approvedAt = LocalDateTime.of(2026, 1, 10, 12, 0);

            estimate.approve(approvedAt);

            assertEquals(EstimateStatus.APPROVED, estimate.getStatus());
            assertEquals(approvedAt, estimate.getApprovedAt());
        }

        @Test
        @DisplayName("rejects a pending estimate")
        void rejectsPendingEstimate() {
            Estimate estimate = pendingEstimate();

            estimate.reject();

            assertEquals(EstimateStatus.REJECTED, estimate.getStatus());
        }

        @Test
        @DisplayName("does not decide an estimate twice")
        void rejectsSecondDecision() {
            Estimate estimate = pendingEstimate();
            estimate.reject();

            assertThrows(EstimateAlreadyDecidedException.class,
                    () -> estimate.approve(LocalDateTime.of(2026, 1, 10, 12, 0)));
        }
    }

    @Nested
    @DisplayName("reserva de estoque")
    class PartsReservation {

        private static final LocalDateTime RESERVED_AT = LocalDateTime.of(2026, 1, 10, 9, 0);

        @Test
        @DisplayName("baixa o saldo de todas as peças e devolve as afetadas")
        void reservesEveryPart() {
            Part brakePad = part("Pastilha", 10);
            Part filter = part("Filtro", 8);
            Estimate estimate = estimateWith(EstimateItem.create(brakePad, 4, null),
                    EstimateItem.create(filter, 2, null));

            List<Part> reserved = estimate.reserveParts(RESERVED_AT);

            assertEquals(List.of(brakePad, filter), reserved);
            assertEquals(6, brakePad.getStockQuantity());
            assertEquals(6, filter.getStockQuantity());
            assertTrue(estimate.hasReservedParts());
        }

        @Test
        @DisplayName("não baixa nenhuma peça quando uma delas não tem saldo suficiente")
        void reservesAllPartsOrNone() {
            Part brakePad = part("Pastilha", 10);
            Part filter = part("Filtro", 1);
            Estimate estimate = estimateWith(EstimateItem.create(brakePad, 4, null),
                    EstimateItem.create(filter, 2, null));

            assertThrows(InsufficientPartStockException.class, () -> estimate.reserveParts(RESERVED_AT));

            assertEquals(10, brakePad.getStockQuantity());
            assertEquals(1, filter.getStockQuantity());
            assertFalse(estimate.hasReservedParts());
        }

        @Test
        @DisplayName("não reserva de novo um orçamento já reservado")
        void doesNotReserveTwice() {
            Part brakePad = part("Pastilha", 10);
            Estimate estimate = estimateWith(EstimateItem.create(brakePad, 4, null));
            estimate.reserveParts(RESERVED_AT);

            assertEquals(List.of(), estimate.reserveParts(RESERVED_AT.plusHours(1)));
            assertEquals(6, brakePad.getStockQuantity());
        }

        @Test
        @DisplayName("devolve ao estoque as peças reservadas e encerra a reserva")
        void restoresReservedParts() {
            Part brakePad = part("Pastilha", 10);
            Part filter = part("Filtro", 8);
            Estimate estimate = estimateWith(EstimateItem.create(brakePad, 4, null),
                    EstimateItem.create(filter, 2, null));
            estimate.reserveParts(RESERVED_AT);

            List<Part> restored = estimate.restoreParts();

            assertEquals(List.of(brakePad, filter), restored);
            assertEquals(10, brakePad.getStockQuantity());
            assertEquals(8, filter.getStockQuantity());
            assertFalse(estimate.hasReservedParts());
        }

        @Test
        @DisplayName("não devolve saldo de um orçamento que nunca reservou peças")
        void restoresNothingWithoutReservation() {
            Part brakePad = part("Pastilha", 10);
            Estimate estimate = estimateWith(EstimateItem.create(brakePad, 4, null));

            assertEquals(List.of(), estimate.restoreParts());
            assertEquals(10, brakePad.getStockQuantity());
        }

        @Test
        @DisplayName("não reserva peças de um orçamento já decidido")
        void rejectsReservationOfDecidedEstimate() {
            Estimate estimate = estimateWith(EstimateItem.create(part("Pastilha", 10), 4, null));
            estimate.reject();

            assertThrows(EstimateAlreadyDecidedException.class, () -> estimate.reserveParts(RESERVED_AT));
        }

        private Estimate estimateWith(EstimateItem... items) {
            return Estimate.create(new WorkOrder(), List.of(items), List.of());
        }

        private Part part(String name, int stockQuantity) {
            return new Part(name, "desc", new BigDecimal("25.00"), stockQuantity, "UN");
        }
    }

    private Estimate pendingEstimate() {
        return Estimate.create(new WorkOrder(), List.of(), List.of());
    }

    private EstimateItem item(String price, int quantity) {
        Part part = new Part("Part", "desc", new BigDecimal(price), 10, "UN");
        return EstimateItem.create(part, quantity, null);
    }
}
