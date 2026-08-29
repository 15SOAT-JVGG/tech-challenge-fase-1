package br.com.fiap.postech.soat16.fase1.workorder.application.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.customer.domain.model.Customer;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.workorder.application.result.WorkOrderResult;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrder;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderPriority;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderStatus;

@DisplayName("WorkOrderMapper — Unit Tests")
class WorkOrderMapperTest {

    private final WorkOrderMapper mapper = new WorkOrderMapper() { };

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("should map all fields from entity to response")
        void shouldMapEntityToResponse() {
            UUID id = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();
            UUID vehicleId = UUID.randomUUID();
            Customer customer = new Customer();
            customer.setId(customerId);
            Vehicle vehicle = new Vehicle();
            vehicle.setId(vehicleId);

            WorkOrder entity = new WorkOrder();
            entity.setId(id);
            entity.setCustomer(customer);
            entity.setVehicle(vehicle);
            entity.setDescription("Troca de pastilhas de freio");
            entity.setPriority(WorkOrderPriority.HIGH);
            entity.setStatus(WorkOrderStatus.RECEIVED);

            WorkOrderResult result = mapper.toResult(entity);

            assertNotNull(result);
            assertEquals(id, result.workOrderId());
            assertEquals(customerId, result.customerId());
            assertEquals(vehicleId, result.vehicleId());
            assertEquals("Troca de pastilhas de freio", result.description());
            assertEquals(WorkOrderPriority.HIGH, result.priority());
            assertEquals(WorkOrderStatus.RECEIVED, result.status());
        }
    }

}
