package br.com.fiap.postech.soat16.fase1.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.model.Estimate;
import br.com.fiap.postech.soat16.fase1.model.enums.EstimateStatus;
import br.com.fiap.postech.soat16.fase1.model.WorkOrder;
import br.com.fiap.postech.soat16.fase1.model.enums.WorkOrderPriority;
import br.com.fiap.postech.soat16.fase1.model.enums.WorkOrderStatus;

@DisplayName("NotificationService — Unit Tests")
class NotificationServiceTest {

    private final NotificationService service = new NotificationService();

    private WorkOrder workOrder() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(UUID.randomUUID());
        workOrder.setPriority(WorkOrderPriority.MEDIUM);
        workOrder.setStatus(WorkOrderStatus.WAITING_APPROVAL);
        return workOrder;
    }

    private Estimate estimate(WorkOrder workOrder) {
        Estimate estimate = new Estimate();
        estimate.setId(UUID.randomUUID());
        estimate.setWorkOrder(workOrder);
        estimate.setStatus(EstimateStatus.PENDING);
        estimate.setTotalAmount(new BigDecimal("150.00"));
        return estimate;
    }

    @Nested
    @DisplayName("notifyEstimateReady")
    class NotifyEstimateReady {

        @Test
        @DisplayName("completes without error for a valid work order and estimate")
        void completesWithoutError() {
            WorkOrder workOrder = workOrder();
            Estimate estimate = estimate(workOrder);

            assertDoesNotThrow(() -> service.notifyEstimateReady(workOrder, estimate).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("notifyWorkOrderCompleted")
    class NotifyWorkOrderCompleted {

        @Test
        @DisplayName("completes without error for a valid work order")
        void completesWithoutError() {
            WorkOrder workOrder = workOrder();
            workOrder.setStatus(WorkOrderStatus.COMPLETED);

            assertDoesNotThrow(() -> service.notifyWorkOrderCompleted(workOrder).await().indefinitely());
        }
    }
}
