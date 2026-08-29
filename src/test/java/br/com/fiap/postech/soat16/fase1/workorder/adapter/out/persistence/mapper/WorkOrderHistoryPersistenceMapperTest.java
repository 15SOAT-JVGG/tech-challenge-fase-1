package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.entity.WorkOrderHistoryJpaEntity;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrder;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrderHistory;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderStatus;

@DisplayName("WorkOrderHistoryPersistenceMapper — Unit Tests")
class WorkOrderHistoryPersistenceMapperTest {

    private static final UUID WORK_ORDER_ID = UUID.randomUUID();
    private static final LocalDateTime CHANGED_AT = LocalDateTime.now();

    private WorkOrderHistory domain() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(WORK_ORDER_ID);

        WorkOrderHistory history = new WorkOrderHistory();
        history.setWorkOrder(workOrder);
        history.setPreviousStatus(WorkOrderStatus.RECEIVED);
        history.setNewStatus(WorkOrderStatus.DIAGNOSIS);
        history.setChangedAt(CHANGED_AT);
        return history;
    }

    @Test
    @DisplayName("maps the transition and references the work order by id")
    void mapsTransitionAndWorkOrderReference() {
        WorkOrderHistoryJpaEntity entity = WorkOrderHistoryPersistenceMapper.toJpaEntity(domain());

        assertEquals(WORK_ORDER_ID, entity.getWorkOrder().getId());
        assertEquals(WorkOrderStatus.RECEIVED, entity.getPreviousStatus());
        assertEquals(WorkOrderStatus.DIAGNOSIS, entity.getNewStatus());
        assertEquals(CHANGED_AT, entity.getChangedAt());
    }

    @Test
    @DisplayName("a history entry without a work order maps to a null reference")
    void historyWithoutWorkOrderMapsToNullReference() {
        WorkOrderHistory history = domain();
        history.setWorkOrder(null);

        assertNull(WorkOrderHistoryPersistenceMapper.toJpaEntity(history).getWorkOrder());
    }

    @Test
    @DisplayName("copyGeneratedState returns the generated id to the domain")
    void copyGeneratedStateFeedsBackId() {
        WorkOrderHistoryJpaEntity entity = new WorkOrderHistoryJpaEntity();
        UUID generatedId = UUID.randomUUID();
        entity.setId(generatedId);

        WorkOrderHistory history = domain();
        WorkOrderHistoryPersistenceMapper.copyGeneratedState(entity, history);

        assertEquals(generatedId, history.getId());
    }

    @Test
    @DisplayName("null input maps to null")
    void nullInputMapsToNull() {
        assertNull(WorkOrderHistoryPersistenceMapper.toJpaEntity(null));
    }
}
