package br.com.fiap.postech.soat16.fase1.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.model.Customer;
import br.com.fiap.postech.soat16.fase1.model.enums.DocumentType;
import br.com.fiap.postech.soat16.fase1.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.model.enums.VehicleType;
import br.com.fiap.postech.soat16.fase1.model.WorkOrder;
import br.com.fiap.postech.soat16.fase1.model.enums.WorkOrderPriority;
import br.com.fiap.postech.soat16.fase1.model.enums.WorkOrderStatus;
import br.com.fiap.postech.soat16.fase1.security.PostgresTestResource;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@DisplayName("WorkOrderRepository — Integration Tests")
class WorkOrderRepositoryIT {

    @Inject
    WorkOrderRepository repository;

    @Inject
    CustomerRepository customerRepository;

    @Inject
    VehicleRepository vehicleRepository;

    private <T> T inTransaction(Supplier<Uni<T>> action) {
        try {
            return VertxContextSupport.subscribeAndAwait(() -> Panache.withTransaction(action));
        } catch (Throwable e) {
            throw new IllegalStateException("Falha ao preparar dados de teste", e);
        }
    }

    private Customer seedCustomer() {
        Customer customer = new Customer();
        customer.setFirstName("Maria");
        customer.setLastName("Silva");
        customer.setPhoneNumber("+5511999999999");
        customer.setDocument("DOC-" + UUID.randomUUID());
        customer.setDocumentType(DocumentType.CPF);
        return inTransaction(() -> customerRepository.persist(customer));
    }

    private Vehicle seedVehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(uniquePlate());
        vehicle.setManufacturer("Fiat");
        vehicle.setModel("Uno");
        vehicle.setColor("Branco");
        vehicle.setYear(2020);
        vehicle.setKmDriven(10_000L);
        vehicle.setType(VehicleType.CAR);
        return inTransaction(() -> vehicleRepository.persist(vehicle));
    }

    private static final java.util.concurrent.atomic.AtomicInteger PLATE_COUNTER = new java.util.concurrent.atomic.AtomicInteger();

    // Matches Vehicle's license plate pattern (^[A-Z]{3}\d[A-Z\d]\d{2}$), e.g. "WOR9A01".
    private static String uniquePlate() {
        return "WOR9A" + String.format("%02d", PLATE_COUNTER.incrementAndGet() % 100);
    }

    private WorkOrder seed(WorkOrderPriority priority, WorkOrderStatus status, LocalDateTime closedAt) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setCustomer(seedCustomer());
        workOrder.setVehicle(seedVehicle());
        workOrder.setDescription("Revisao geral");
        workOrder.setPriority(priority);
        workOrder.setStatus(status);
        workOrder.setOpenedAt(LocalDateTime.now());
        workOrder.setClosedAt(closedAt);
        return inTransaction(() -> repository.persist(workOrder));
    }

    @Nested
    @DisplayName("findPage")
    class FindPage {

        @Test
        @DisplayName("orders results by priority (URGENT first, LOW last)")
        void ordersByPriorityUrgentFirst() {
            seed(WorkOrderPriority.LOW, WorkOrderStatus.RECEIVED, null);
            WorkOrder urgent = seed(WorkOrderPriority.URGENT, WorkOrderStatus.RECEIVED, null);

            List<WorkOrder> page = inTransaction(() -> repository.findPage(0, 100));

            int urgentIndex = indexOf(page, urgent.getId());
            int lowestPriorityIndex = page.size() - 1;
            assertTrue(urgentIndex <= lowestPriorityIndex);
            assertEquals(WorkOrderPriority.URGENT, page.get(urgentIndex).getPriority());
        }

        private int indexOf(List<WorkOrder> page, UUID id) {
            for (int i = 0; i < page.size(); i++) {
                if (page.get(i).getId().equals(id)) {
                    return i;
                }
            }
            throw new IllegalStateException("work order not found in page");
        }
    }

    @Nested
    @DisplayName("findByWorkOrderId")
    class FindByWorkOrderId {

        @Test
        @DisplayName("finds a persisted work order by id")
        void findsById() {
            WorkOrder workOrder = seed(WorkOrderPriority.MEDIUM, WorkOrderStatus.RECEIVED, null);

            WorkOrder found = inTransaction(() -> repository.findByWorkOrderId(workOrder.getId()));

            assertEquals(workOrder.getId(), found.getId());
        }

        @Test
        @DisplayName("returns null when the id does not exist")
        void returnsNullWhenNotFound() {
            assertNull(inTransaction(() -> repository.findByWorkOrderId(UUID.randomUUID())));
        }
    }

    @Nested
    @DisplayName("findClosed")
    class FindClosed {

        @Test
        @DisplayName("includes only work orders with both openedAt and closedAt set")
        void includesOnlyClosedWorkOrders() {
            WorkOrder closed = seed(WorkOrderPriority.MEDIUM, WorkOrderStatus.DELIVERED, LocalDateTime.now());
            seed(WorkOrderPriority.MEDIUM, WorkOrderStatus.RECEIVED, null);

            List<WorkOrder> result = inTransaction(() -> repository.findClosed());

            assertTrue(result.stream().anyMatch(w -> w.getId().equals(closed.getId())));
            assertTrue(result.stream().allMatch(w -> w.getClosedAt() != null));
        }
    }
}
