package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.servicecatalog.domain.model.ServiceItem;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.entity.WorkOrderServiceJpaEntity;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrder;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrderService;

@DisplayName("WorkOrderServicePersistenceMapper — Unit Tests")
class WorkOrderServicePersistenceMapperTest {

    private static final UUID WORK_ORDER_ID = UUID.randomUUID();
    private static final UUID SERVICE_ITEM_ID = UUID.randomUUID();
    private static final LocalDateTime PERFORMED_AT = LocalDateTime.now();

    private WorkOrderService domain() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(WORK_ORDER_ID);

        ServiceItem serviceItem = new ServiceItem();
        serviceItem.setId(SERVICE_ITEM_ID);

        WorkOrderService service = new WorkOrderService();
        service.setWorkOrder(workOrder);
        service.setServiceItem(serviceItem);
        service.setDescription("Troca de oleo");
        service.setPrice(new BigDecimal("90.00"));
        service.setPerformedAt(PERFORMED_AT);
        return service;
    }

    @Test
    @DisplayName("round trip preserves fields and the related aggregate ids")
    void roundTripPreservesFields() {
        WorkOrderService result = WorkOrderServicePersistenceMapper.toDomain(
                WorkOrderServicePersistenceMapper.toJpaEntity(domain()));

        assertEquals(WORK_ORDER_ID, result.getWorkOrder().getId());
        assertEquals(SERVICE_ITEM_ID, result.getServiceItem().getId());
        assertEquals("Troca de oleo", result.getDescription());
        assertEquals(new BigDecimal("90.00"), result.getPrice());
        assertEquals(PERFORMED_AT, result.getPerformedAt());
    }

    @Test
    @DisplayName("a free-form service maps to a null catalog item")
    void freeFormServiceMapsToNullCatalogItem() {
        WorkOrderService service = domain();
        service.setServiceItem(null);

        WorkOrderServiceJpaEntity entity = WorkOrderServicePersistenceMapper.toJpaEntity(service);

        assertNull(entity.getServiceItem());
        assertNull(WorkOrderServicePersistenceMapper.toDomain(entity).getServiceItem());
    }

    @Test
    @DisplayName("copyGeneratedState returns the generated id to the domain")
    void copyGeneratedStateFeedsBackId() {
        WorkOrderServiceJpaEntity entity = new WorkOrderServiceJpaEntity();
        UUID generatedId = UUID.randomUUID();
        entity.setId(generatedId);

        WorkOrderService service = domain();
        WorkOrderServicePersistenceMapper.copyGeneratedState(entity, service);

        assertEquals(generatedId, service.getId());
    }

    @Test
    @DisplayName("null inputs map to null")
    void nullInputsMapToNull() {
        assertNull(WorkOrderServicePersistenceMapper.toDomain(null));
        assertNull(WorkOrderServicePersistenceMapper.toJpaEntity(null));
    }
}
