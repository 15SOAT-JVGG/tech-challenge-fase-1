package br.com.fiap.postech.soat16.fase1.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.dto.request.WorkOrderRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkOrderResponseDto;
import br.com.fiap.postech.soat16.fase1.model.Customer;
import br.com.fiap.postech.soat16.fase1.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.model.WorkOrder;
import br.com.fiap.postech.soat16.fase1.model.enums.WorkOrderPriority;
import br.com.fiap.postech.soat16.fase1.model.enums.WorkOrderStatus;

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

            WorkOrderResponseDto result = mapper.toResponse(entity);

            assertNotNull(result);
            assertEquals(id, result.workOrderId());
            assertEquals(customerId, result.customerId());
            assertEquals(vehicleId, result.vehicleId());
            assertEquals("Troca de pastilhas de freio", result.description());
            assertEquals(WorkOrderPriority.HIGH, result.priority());
            assertEquals(WorkOrderStatus.RECEIVED, result.status());
        }
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("should apply default priority, RECEIVED status and openedAt when priority is missing")
        void shouldApplyDefaultsWhenPriorityMissing() {
            WorkOrderRequestDto request = new WorkOrderRequestDto(
                    UUID.randomUUID(), UUID.randomUUID(), "Revisao geral", null, null);
            Customer customer = new Customer();
            Vehicle vehicle = new Vehicle();

            WorkOrder result = mapper.toEntity(request, customer, vehicle, null);

            assertNotNull(result);
            assertEquals("Revisao geral", result.getDescription());
            assertEquals(WorkOrderPriority.MEDIUM, result.getPriority());
            assertEquals(WorkOrderStatus.RECEIVED, result.getStatus());
            assertEquals(customer, result.getCustomer());
            assertEquals(vehicle, result.getVehicle());
            assertNotNull(result.getOpenedAt());
        }

        @Test
        @DisplayName("should keep requested priority when provided")
        void shouldKeepRequestedPriority() {
            WorkOrderRequestDto request = new WorkOrderRequestDto(
                    UUID.randomUUID(), UUID.randomUUID(), "Revisao geral", WorkOrderPriority.URGENT, null);

            WorkOrder result = mapper.toEntity(request, new Customer(), new Vehicle(), null);

            assertEquals(WorkOrderPriority.URGENT, result.getPriority());
        }
    }
}
