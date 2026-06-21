package br.com.fiap.postech.soat16.fase1.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
            WorkOrder open = workOrder(id, WorkOrderStatus.OPEN);
            WorkOrder completed = workOrder(id, WorkOrderStatus.COMPLETED);

            assertEquals(open, completed);
            assertEquals(open.hashCode(), completed.hashCode());
        }

        @Test
        @DisplayName("work orders with different ids are not equal")
        void notEqualWhenIdsDiffer() {
            WorkOrder a = workOrder(UUID.randomUUID(), WorkOrderStatus.OPEN);
            WorkOrder b = workOrder(UUID.randomUUID(), WorkOrderStatus.OPEN);

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("a work order is not equal to null or to an unrelated type")
        void notEqualToNullOrUnrelatedType() {
            WorkOrder a = workOrder(UUID.randomUUID(), WorkOrderStatus.OPEN);

            assertNotEquals(null, a);
            assertNotEquals("not-a-work-order", a);
        }
    }

    @Nested
    @DisplayName("lifecycle fields")
    class LifecycleFields {

        @Test
        @DisplayName("closedAt and finalValue are settable once the work order is delivered")
        void closingFieldsAreSettable() {
            WorkOrder workOrder = workOrder(UUID.randomUUID(), WorkOrderStatus.DELIVERED);
            LocalDateTime closedAt = LocalDateTime.now();

            workOrder.setClosedAt(closedAt);
            workOrder.setFinalValue(new BigDecimal("180.00"));

            assertEquals(WorkOrderStatus.DELIVERED, workOrder.getStatus());
            assertEquals(closedAt, workOrder.getClosedAt());
            assertEquals(new BigDecimal("180.00"), workOrder.getFinalValue());
        }
    }
}
