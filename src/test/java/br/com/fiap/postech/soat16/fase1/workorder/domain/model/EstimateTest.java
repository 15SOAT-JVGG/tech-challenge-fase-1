package br.com.fiap.postech.soat16.fase1.workorder.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.model.Part;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.EstimateAlreadyDecidedException;
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

    private Estimate pendingEstimate() {
        return Estimate.create(new WorkOrder(), List.of(), List.of());
    }

    private EstimateItem item(String price, int quantity) {
        Part part = new Part("Part", "desc", new BigDecimal(price), 10, "UN");
        return EstimateItem.create(part, quantity, null);
    }
}
