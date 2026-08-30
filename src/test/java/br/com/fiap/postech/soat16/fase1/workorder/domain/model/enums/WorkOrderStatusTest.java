package br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("WorkOrderStatus")
class WorkOrderStatusTest {

    @Nested
    @DisplayName("operationalQueue")
    class OperationalQueue {

        @Test
        @DisplayName("deve ordenar do estágio mais avançado para o recém-recebido")
        void ordersFromTheMostAdvancedStage() {
            assertEquals(
                    List.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.WAITING_APPROVAL,
                            WorkOrderStatus.DIAGNOSIS, WorkOrderStatus.RECEIVED),
                    WorkOrderStatus.operationalQueue());
        }

        @Test
        @DisplayName("deve deixar de fora as ordens já encerradas")
        void excludesClosedStatuses() {
            assertFalse(WorkOrderStatus.operationalQueue().contains(WorkOrderStatus.COMPLETED));
            assertFalse(WorkOrderStatus.operationalQueue().contains(WorkOrderStatus.DELIVERED));
        }

        @Test
        @DisplayName("deve ser imutável")
        void isImmutable() {
            List<WorkOrderStatus> queue = WorkOrderStatus.operationalQueue();

            assertThrows(UnsupportedOperationException.class, () -> queue.add(WorkOrderStatus.DELIVERED));
        }
    }
}
