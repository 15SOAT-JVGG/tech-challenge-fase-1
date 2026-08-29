package br.com.fiap.postech.soat16.fase1.workorder.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.customer.domain.model.Customer;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.EstimateNotApprovedException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.InvalidWorkOrderStatusTransitionException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.WorkOrderLockedException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.EstimateStatus;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderPriority;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderStatus;

@DisplayName("WorkOrder model — Unit Tests")
class WorkOrderTest {

    private WorkOrder workOrder(UUID id, WorkOrderStatus status) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(id);
        workOrder.setCustomer(new Customer());
        workOrder.setVehicle(new Vehicle());
        workOrder.setDescription("Troca de pastilhas de freio");
        workOrder.setPriority(WorkOrderPriority.HIGH);
        workOrder.setStatus(status);
        workOrder.setOpenedAt(LocalDateTime.now());
        workOrder.setEstimatedValue(new BigDecimal("150.00"));
        return workOrder;
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        @DisplayName("work orders with the same id are equal regardless of other fields")
        void equalByIdRegardlessOfOtherFields() {
            UUID id = UUID.randomUUID();
            WorkOrder open = workOrder(id, WorkOrderStatus.RECEIVED);
            WorkOrder completed = workOrder(id, WorkOrderStatus.COMPLETED);

            assertEquals(open, completed);
            assertEquals(open.hashCode(), completed.hashCode());
        }

        @Test
        @DisplayName("work orders with different ids are not equal")
        void notEqualWhenIdsDiffer() {
            WorkOrder a = workOrder(UUID.randomUUID(), WorkOrderStatus.RECEIVED);
            WorkOrder b = workOrder(UUID.randomUUID(), WorkOrderStatus.RECEIVED);

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("a work order is not equal to null or to an unrelated type")
        void notEqualToNullOrUnrelatedType() {
            WorkOrder a = workOrder(UUID.randomUUID(), WorkOrderStatus.RECEIVED);

            assertNotEquals(null, a);
            assertNotEquals("not-a-work-order", a);
        }
    }

    @Nested
    @DisplayName("opening")
    class Opening {

        @Test
        @DisplayName("opens with domain defaults")
        void opensWithDomainDefaults() {
            Customer customer = new Customer();
            Vehicle vehicle = new Vehicle();
            LocalDateTime openedAt = LocalDateTime.of(2026, 1, 10, 9, 30);

            WorkOrder result = WorkOrder.open(customer, vehicle, null, "Revisao geral", null, openedAt);

            assertEquals(customer, result.getCustomer());
            assertEquals(vehicle, result.getVehicle());
            assertEquals("Revisao geral", result.getDescription());
            assertEquals(WorkOrderPriority.MEDIUM, result.getPriority());
            assertEquals(WorkOrderStatus.RECEIVED, result.getStatus());
            assertEquals(openedAt, result.getOpenedAt());
        }
    }

    @Nested
    @DisplayName("status transitions")
    class StatusTransitions {

        private static final LocalDateTime CHANGED_AT = LocalDateTime.of(2026, 1, 10, 10, 0);

        @Test
        @DisplayName("advances one lifecycle step and returns its history")
        void advancesAndReturnsHistory() {
            WorkOrder order = workOrder(UUID.randomUUID(), WorkOrderStatus.RECEIVED);

            WorkOrderHistory history = order.transitionTo(WorkOrderStatus.DIAGNOSIS, false, CHANGED_AT);

            assertEquals(WorkOrderStatus.DIAGNOSIS, order.getStatus());
            assertEquals(WorkOrderStatus.RECEIVED, history.getPreviousStatus());
            assertEquals(WorkOrderStatus.DIAGNOSIS, history.getNewStatus());
            assertEquals(CHANGED_AT, history.getChangedAt());
            assertEquals(order, history.getWorkOrder());
        }

        @Test
        @DisplayName("rejects skipped lifecycle steps")
        void rejectsSkippedSteps() {
            WorkOrder order = workOrder(UUID.randomUUID(), WorkOrderStatus.RECEIVED);

            assertThrows(InvalidWorkOrderStatusTransitionException.class,
                    () -> order.transitionTo(WorkOrderStatus.APPROVED, true, CHANGED_AT));
        }

        @Test
        @DisplayName("requires an approved estimate before approval or execution")
        void requiresApprovedEstimate() {
            WorkOrder order = workOrder(UUID.randomUUID(), WorkOrderStatus.WAITING_APPROVAL);

            assertThrows(EstimateNotApprovedException.class,
                    () -> order.transitionTo(WorkOrderStatus.APPROVED, false, CHANGED_AT));
        }

        @Test
        @DisplayName("requires the dedicated close behavior to complete")
        void rejectsGenericCompletion() {
            WorkOrder order = workOrder(UUID.randomUUID(), WorkOrderStatus.IN_PROGRESS);

            assertThrows(InvalidWorkOrderStatusTransitionException.class,
                    () -> order.transitionTo(WorkOrderStatus.COMPLETED, true, CHANGED_AT));
        }

        @Test
        @DisplayName("locks terminal work orders")
        void locksTerminalOrders() {
            WorkOrder order = workOrder(UUID.randomUUID(), WorkOrderStatus.DELIVERED);

            assertThrows(WorkOrderLockedException.class,
                    () -> order.transitionTo(WorkOrderStatus.CANCELLED, false, CHANGED_AT));
        }
    }

    @Nested
    @DisplayName("estimate lifecycle")
    class EstimateLifecycle {

        private static final LocalDateTime CHANGED_AT = LocalDateTime.of(2026, 1, 10, 10, 0);

        @Test
        @DisplayName("registering an estimate updates value and enters waiting approval")
        void registersEstimate() {
            WorkOrder order = workOrder(UUID.randomUUID(), WorkOrderStatus.DIAGNOSIS);
            Estimate estimate = estimate(order, "175.00");

            var history = order.registerEstimate(estimate, CHANGED_AT);

            assertEquals(new BigDecimal("175.00"), order.getEstimatedValue());
            assertEquals(WorkOrderStatus.WAITING_APPROVAL, order.getStatus());
            assertTrue(history.isPresent());
        }

        @Test
        @DisplayName("approval advances a waiting work order")
        void registersApproval() {
            WorkOrder order = workOrder(UUID.randomUUID(), WorkOrderStatus.WAITING_APPROVAL);
            Estimate estimate = estimate(order, "175.00");
            estimate.setStatus(EstimateStatus.APPROVED);

            var history = order.registerEstimateApproval(estimate, CHANGED_AT);

            assertEquals(WorkOrderStatus.APPROVED, order.getStatus());
            assertTrue(history.isPresent());
            assertTrue(order.hasReservedStock());
        }

        @Test
        @DisplayName("rejection returns a waiting work order to diagnosis")
        void registersRejection() {
            WorkOrder order = workOrder(UUID.randomUUID(), WorkOrderStatus.WAITING_APPROVAL);

            var history = order.registerEstimateRejection(CHANGED_AT);

            assertEquals(WorkOrderStatus.DIAGNOSIS, order.getStatus());
            assertTrue(history.isPresent());
        }
    }

    @Nested
    @DisplayName("closing")
    class Closing {

        @Test
        @DisplayName("uses estimated value when final value is omitted")
        void closesUsingEstimatedValue() {
            WorkOrder order = workOrder(UUID.randomUUID(), WorkOrderStatus.IN_PROGRESS);
            LocalDateTime closedAt = LocalDateTime.of(2026, 1, 10, 17, 0);

            WorkOrderHistory history = order.close(null, true, closedAt);

            assertEquals(WorkOrderStatus.COMPLETED, order.getStatus());
            assertEquals(new BigDecimal("150.00"), order.getFinalValue());
            assertEquals(closedAt, order.getClosedAt());
            assertEquals(WorkOrderStatus.COMPLETED, history.getNewStatus());
        }

        @Test
        @DisplayName("requires an approved estimate")
        void rejectsCloseWithoutApprovedEstimate() {
            WorkOrder order = workOrder(UUID.randomUUID(), WorkOrderStatus.IN_PROGRESS);

            assertThrows(EstimateNotApprovedException.class,
                    () -> order.close(null, false, LocalDateTime.of(2026, 1, 10, 17, 0)));
        }
    }

    private Estimate estimate(WorkOrder order, String total) {
        Estimate estimate = new Estimate();
        estimate.setWorkOrder(order);
        estimate.setStatus(EstimateStatus.PENDING);
        estimate.setTotalAmount(new BigDecimal(total));
        return estimate;
    }
}
