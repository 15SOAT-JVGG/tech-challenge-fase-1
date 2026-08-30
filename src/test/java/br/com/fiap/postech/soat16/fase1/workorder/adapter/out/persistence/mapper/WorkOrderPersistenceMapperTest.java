package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.customer.domain.model.Customer;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.enums.VehicleType;
import br.com.fiap.postech.soat16.fase1.worker.domain.model.Worker;
import br.com.fiap.postech.soat16.fase1.worker.domain.model.enums.WorkerProfile;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.entity.WorkOrderJpaEntity;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrder;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderPriority;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderStatus;

@DisplayName("WorkOrderPersistenceMapper — Unit Tests")
class WorkOrderPersistenceMapperTest {

    private static final UUID WORK_ORDER_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID VEHICLE_ID = UUID.randomUUID();
    private static final UUID WORKER_ID = UUID.randomUUID();
    private static final LocalDateTime OPENED_AT = LocalDateTime.now();
    private static final OffsetDateTime NOW = OffsetDateTime.now();

    private WorkOrder domain() {
        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);

        Vehicle vehicle = Vehicle.create(
                customer, "ABC1D23", "Fiat", "Uno", "Branco", 2020, 10_000L, VehicleType.CAR);
        vehicle.setId(VEHICLE_ID);

        Worker worker = Worker.create(
                WorkerProfile.MECHANIC, "Joao", "Souza", "joao@example.com", "119", "hash");
        worker.setId(WORKER_ID);

        WorkOrder workOrder = WorkOrder.open(
                customer, vehicle, worker, "Revisao geral", WorkOrderPriority.HIGH, OPENED_AT);
        workOrder.setId(WORK_ORDER_ID);
        workOrder.setEstimatedValue(new BigDecimal("250.00"));
        workOrder.setCreatedAt(NOW);
        workOrder.setUpdatedAt(NOW);
        workOrder.setCreatedBy("system");
        workOrder.setUpdatedBy("system");
        return workOrder;
    }

    @Test
    @DisplayName("round trip preserves fields and the related aggregate ids")
    void roundTripPreservesFields() {
        WorkOrder result = WorkOrderPersistenceMapper.toDomain(
                WorkOrderPersistenceMapper.toJpaEntity(domain()));

        assertEquals(WORK_ORDER_ID, result.getId());
        assertEquals(CUSTOMER_ID, result.getCustomer().getId());
        assertEquals(VEHICLE_ID, result.getVehicle().getId());
        assertEquals(WORKER_ID, result.getAssignedWorker().getId());
        assertEquals("Revisao geral", result.getDescription());
        assertEquals(WorkOrderPriority.HIGH, result.getPriority());
        assertEquals(WorkOrderStatus.RECEIVED, result.getStatus());
        assertEquals(OPENED_AT, result.getOpenedAt());
        assertEquals(new BigDecimal("250.00"), result.getEstimatedValue());
        assertEquals(NOW, result.getCreatedAt());
    }

    @Test
    @DisplayName("an unassigned work order maps to a null worker")
    void unassignedWorkOrderMapsToNullWorker() {
        WorkOrder workOrder = domain();
        workOrder.setAssignedWorker(null);

        WorkOrderJpaEntity entity = WorkOrderPersistenceMapper.toJpaEntity(workOrder);

        assertNull(entity.getAssignedWorker());
        assertNull(WorkOrderPersistenceMapper.toDomain(entity).getAssignedWorker());
    }

    @Test
    @DisplayName("copyState copia o cancelamento sem alterar a identidade")
    void copyStateCarriesStatusChanges() {
        WorkOrderJpaEntity entity = WorkOrderPersistenceMapper.toJpaEntity(domain());
        WorkOrder updated = domain();
        updated.setStatus(WorkOrderStatus.COMPLETED);
        updated.setClosedAt(OPENED_AT.plusHours(3));
        updated.setCancelledAt(OPENED_AT.plusHours(3));
        updated.setFinalValue(new BigDecimal("300.00"));

        WorkOrderPersistenceMapper.copyState(updated, entity);

        assertEquals(WORK_ORDER_ID, entity.getId());
        assertEquals(WorkOrderStatus.COMPLETED, entity.getStatus());
        assertEquals(OPENED_AT.plusHours(3), entity.getClosedAt());
        assertEquals(OPENED_AT.plusHours(3), entity.getCancelledAt());
        assertEquals(new BigDecimal("300.00"), entity.getFinalValue());
    }

    @Test
    @DisplayName("conversão de ida e volta preserva a data do cancelamento")
    void roundTripPreservesCancellationTime() {
        WorkOrder cancelled = domain();
        cancelled.setStatus(WorkOrderStatus.COMPLETED);
        cancelled.setClosedAt(OPENED_AT.plusHours(1));
        cancelled.setCancelledAt(OPENED_AT.plusHours(1));

        WorkOrder result = WorkOrderPersistenceMapper.toDomain(
                WorkOrderPersistenceMapper.toJpaEntity(cancelled));

        assertEquals(cancelled.getCancelledAt(), result.getCancelledAt());
    }

    @Test
    @DisplayName("copyGeneratedState returns identity and auditing to the aggregate")
    void copyGeneratedStateFeedsBackIdentity() {
        WorkOrderJpaEntity entity = new WorkOrderJpaEntity();
        UUID generatedId = UUID.randomUUID();
        entity.setId(generatedId);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        entity.setCreatedBy("system");
        entity.setUpdatedBy("system");

        WorkOrder workOrder = new WorkOrder();
        WorkOrderPersistenceMapper.copyGeneratedState(entity, workOrder);

        assertEquals(generatedId, workOrder.getId());
        assertEquals(NOW, workOrder.getUpdatedAt());
    }

    @Test
    @DisplayName("null inputs map to null")
    void nullInputsMapToNull() {
        assertNull(WorkOrderPersistenceMapper.toDomain(null));
        assertNull(WorkOrderPersistenceMapper.toJpaEntity(null));
        assertNull(WorkOrderPersistenceMapper.toJpaReference(null));
    }
}
