package br.com.fiap.postech.soat16.fase1.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.model.Customer;
import br.com.fiap.postech.soat16.fase1.model.DocumentType;
import br.com.fiap.postech.soat16.fase1.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.model.VehicleType;
import br.com.fiap.postech.soat16.fase1.model.WorkOrder;
import br.com.fiap.postech.soat16.fase1.model.WorkOrderPriority;
import br.com.fiap.postech.soat16.fase1.model.WorkOrderService;
import br.com.fiap.postech.soat16.fase1.model.WorkOrderStatus;
import br.com.fiap.postech.soat16.fase1.security.PostgresTestResource;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@DisplayName("WorkOrderServiceRepository — Integration Tests")
class WorkOrderServiceRepositoryIT {

    @Inject
    WorkOrderServiceRepository repository;

    @Inject
    CustomerRepository customerRepository;

    @Inject
    VehicleRepository vehicleRepository;

    @Inject
    WorkOrderRepository workOrderRepository;

    private <T> T inTransaction(Supplier<Uni<T>> action) {
        try {
            return VertxContextSupport.subscribeAndAwait(() -> Panache.withTransaction(action));
        } catch (Throwable e) {
            throw new IllegalStateException("Falha ao preparar dados de teste", e);
        }
    }

    private WorkOrder seedWorkOrder() {
        Customer customer = new Customer();
        customer.setFirstName("Maria");
        customer.setLastName("Silva");
        customer.setPhoneNumber("+5511999999999");
        customer.setDocument("DOC-" + UUID.randomUUID());
        customer.setDocumentType(DocumentType.CPF);

        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(uniquePlate());
        vehicle.setManufacturer("Fiat");
        vehicle.setModel("Uno");
        vehicle.setColor("Branco");
        vehicle.setYear(2020);
        vehicle.setKmDriven(10_000L);
        vehicle.setType(VehicleType.CAR);

        return inTransaction(() -> customerRepository.persist(customer)
            .chain(c -> vehicleRepository.persist(vehicle)
                .chain(v -> {
                    WorkOrder workOrder = new WorkOrder();
                    workOrder.setCustomer(c);
                    workOrder.setVehicle(v);
                    workOrder.setDescription("Revisao geral");
                    workOrder.setPriority(WorkOrderPriority.MEDIUM);
                    workOrder.setStatus(WorkOrderStatus.IN_PROGRESS);
                    workOrder.setOpenedAt(LocalDateTime.now());
                    return workOrderRepository.persist(workOrder);
                })));
    }

    private static final java.util.concurrent.atomic.AtomicInteger PLATE_COUNTER = new java.util.concurrent.atomic.AtomicInteger();

    // Matches Vehicle's license plate pattern (^[A-Z]{3}\d[A-Z\d]\d{2}$), e.g. "WOS9A01".
    private static String uniquePlate() {
        return "WOS9A" + String.format("%02d", PLATE_COUNTER.incrementAndGet() % 100);
    }

    private WorkOrderService seedService(WorkOrder workOrder, String description) {
        WorkOrderService service = new WorkOrderService();
        service.setWorkOrder(workOrder);
        service.setDescription(description);
        service.setPrice(new BigDecimal("120.00"));
        service.setPerformedAt(LocalDateTime.now());
        return inTransaction(() -> repository.persist(service));
    }

    @Nested
    @DisplayName("findByWorkOrderId")
    class FindByWorkOrderId {

        @Test
        @DisplayName("returns only the labor lines belonging to the given work order")
        void returnsOnlyServicesForGivenWorkOrder() {
            WorkOrder workOrder = seedWorkOrder();
            WorkOrder otherWorkOrder = seedWorkOrder();
            seedService(workOrder, "Troca de oleo");
            seedService(otherWorkOrder, "Alinhamento");

            List<WorkOrderService> services = inTransaction(() -> repository.findByWorkOrderId(workOrder.getId()));

            assertEquals(1, services.size());
            assertEquals("Troca de oleo", services.get(0).getDescription());
        }

        @Test
        @DisplayName("returns an empty list when the work order has no services")
        void returnsEmptyListWhenNoServices() {
            WorkOrder workOrder = seedWorkOrder();

            List<WorkOrderService> services = inTransaction(() -> repository.findByWorkOrderId(workOrder.getId()));

            assertTrue(services.isEmpty());
        }
    }
}
