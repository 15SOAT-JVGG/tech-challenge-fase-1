package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.customer.adapter.out.persistence.CustomerRepository;
import br.com.fiap.postech.soat16.fase1.customer.domain.model.Customer;
import br.com.fiap.postech.soat16.fase1.customer.domain.model.enums.DocumentType;
import br.com.fiap.postech.soat16.fase1.shared.test.infrastructure.PostgresTestResource;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.out.persistence.VehicleRepository;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.enums.VehicleType;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrder;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrderHistory;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderPriority;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderStatus;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@DisplayName("WorkOrderHistoryRepository — Integration Tests")
class WorkOrderHistoryRepositoryIT {

    @Inject
    WorkOrderHistoryRepository repository;

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

    private static final java.util.concurrent.atomic.AtomicInteger PLATE_COUNTER = new java.util.concurrent.atomic.AtomicInteger();

    private static String uniquePlate() {
        return "WOH9A" + String.format("%02d", PLATE_COUNTER.incrementAndGet() % 100);
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
                    workOrder.setStatus(WorkOrderStatus.DIAGNOSIS);
                    workOrder.setOpenedAt(LocalDateTime.now());
                    return workOrderRepository.persist(workOrder);
                })));
    }

    private WorkOrderHistory seedHistory(WorkOrder workOrder) {
        WorkOrderHistory history = new WorkOrderHistory();
        history.setWorkOrder(workOrder);
        history.setPreviousStatus(WorkOrderStatus.RECEIVED);
        history.setNewStatus(WorkOrderStatus.DIAGNOSIS);
        history.setChangedAt(LocalDateTime.now());
        return inTransaction(() -> repository.persist(history));
    }

    @Nested
    @DisplayName("findById / persist")
    class FindByIdAndPersist {

        @Test
        @DisplayName("finds a persisted history entry with its previous/new status")
        void findsById() {
            WorkOrder workOrder = seedWorkOrder();
            WorkOrderHistory history = seedHistory(workOrder);

            WorkOrderHistory found = inTransaction(() -> repository.find("id = ?1", history.getId()).firstResult());

            assertEquals(WorkOrderStatus.RECEIVED, found.getPreviousStatus());
            assertEquals(WorkOrderStatus.DIAGNOSIS, found.getNewStatus());
        }

        @Test
        @DisplayName("returns null when the id does not exist")
        void returnsNullWhenNotFound() {
            assertNull(inTransaction(() -> repository.find("id = ?1", UUID.randomUUID()).firstResult()));
        }
    }
}
