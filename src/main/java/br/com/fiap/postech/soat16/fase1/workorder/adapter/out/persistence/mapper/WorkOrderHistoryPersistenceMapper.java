package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.mapper;

import br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.entity.WorkOrderHistoryJpaEntity;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrderHistory;

public final class WorkOrderHistoryPersistenceMapper {

    private WorkOrderHistoryPersistenceMapper() {
    }

    public static WorkOrderHistoryJpaEntity toJpaEntity(WorkOrderHistory history) {
        if (history == null) {
            return null;
        }
        var entity = new WorkOrderHistoryJpaEntity();
        entity.setId(history.getId());
        entity.setWorkOrder(WorkOrderPersistenceMapper.toJpaReference(
                history.getWorkOrder() != null ? history.getWorkOrder().getId() : null));
        entity.setPreviousStatus(history.getPreviousStatus());
        entity.setNewStatus(history.getNewStatus());
        entity.setChangedAt(history.getChangedAt());
        return entity;
    }

    public static void copyGeneratedState(WorkOrderHistoryJpaEntity source, WorkOrderHistory target) {
        target.setId(source.getId());
    }
}
