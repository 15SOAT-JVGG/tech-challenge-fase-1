package br.com.fiap.postech.soat16.fase1.workorder.application.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.model.ServiceItem;
import br.com.fiap.postech.soat16.fase1.workorder.application.command.AddWorkOrderServiceCommand;
import br.com.fiap.postech.soat16.fase1.workorder.application.result.WorkOrderServiceResult;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrder;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrderService;

@DisplayName("WorkOrderServiceMapper — Unit Tests")
class WorkOrderServiceMapperTest {

    private final WorkOrderServiceMapper mapper = new WorkOrderServiceMapper() { };

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("should map all fields from entity to response")
        void shouldMapEntityToResponse() {
            UUID id = UUID.randomUUID();
            UUID workOrderId = UUID.randomUUID();
            UUID serviceItemId = UUID.randomUUID();
            WorkOrder workOrder = new WorkOrder();
            workOrder.setId(workOrderId);
            ServiceItem serviceItem = new ServiceItem();
            serviceItem.setId(serviceItemId);

            WorkOrderService entity = new WorkOrderService();
            entity.setId(id);
            entity.setWorkOrder(workOrder);
            entity.setDescription("Troca de oleo");
            entity.setPrice(BigDecimal.valueOf(120));
            entity.setServiceItem(serviceItem);

            WorkOrderServiceResult result = mapper.toResult(entity);

            assertNotNull(result);
            assertEquals(id, result.workOrderServiceId());
            assertEquals(workOrderId, result.workOrderId());
            assertEquals("Troca de oleo", result.description());
            assertEquals(BigDecimal.valueOf(120), result.price());
            assertEquals(serviceItemId, result.serviceItemId());
        }
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("should map request fields and set performedAt")
        void shouldMapRequestAndSetPerformedAt() {
            WorkOrder workOrder = new WorkOrder();
            ServiceItem serviceItem = new ServiceItem();
            AddWorkOrderServiceCommand request =
                    new AddWorkOrderServiceCommand("Alinhamento", BigDecimal.valueOf(90), null);

            WorkOrderService result = mapper.toEntity(request, workOrder, serviceItem);

            assertEquals(workOrder, result.getWorkOrder());
            assertEquals("Alinhamento", result.getDescription());
            assertEquals(BigDecimal.valueOf(90), result.getPrice());
            assertEquals(serviceItem, result.getServiceItem());
            assertNotNull(result.getPerformedAt());
        }
    }
}
