package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.customer.adapter.out.persistence.CustomerRepository;
import br.com.fiap.postech.soat16.fase1.customer.domain.model.Customer;
import br.com.fiap.postech.soat16.fase1.customer.domain.model.enums.DocumentType;
import br.com.fiap.postech.soat16.fase1.shared.test.infrastructure.JwtKeyPairTestResource;
import br.com.fiap.postech.soat16.fase1.shared.test.infrastructure.PostgresTestResource;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.out.persistence.VehicleRepository;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.enums.VehicleType;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrder;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderPriority;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderStatus;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@QuarkusTestResource(JwtKeyPairTestResource.class)
@DisplayName("WorkOrderRepository — Integration Tests")
class WorkOrderRepositoryIT {

    @Inject
    WorkOrderRepository repository;

    @Inject
    WorkOrderHistoryRepository historyRepository;

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
        return inTransaction(() -> customerRepository.save(customer));
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
        return inTransaction(() -> vehicleRepository.save(vehicle));
    }

    private static final java.util.concurrent.atomic.AtomicInteger PLATE_COUNTER = new java.util.concurrent.atomic.AtomicInteger();

    private static String uniquePlate() {
        return "WOR9A" + String.format("%02d", PLATE_COUNTER.incrementAndGet() % 100);
    }

    private WorkOrder seed(WorkOrderPriority priority, WorkOrderStatus status, LocalDateTime closedAt) {
        return seed(priority, status, closedAt, LocalDateTime.now());
    }

    private WorkOrder seed(WorkOrderPriority priority, WorkOrderStatus status, LocalDateTime closedAt,
            LocalDateTime openedAt) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setCustomer(seedCustomer());
        workOrder.setVehicle(seedVehicle());
        workOrder.setDescription("Revisao geral");
        workOrder.setPriority(priority);
        workOrder.setStatus(status);
        workOrder.setOpenedAt(openedAt);
        workOrder.setClosedAt(closedAt);
        return inTransaction(() -> repository.save(workOrder));
    }

    @Nested
    @DisplayName("findOperationalQueuePage")
    class FindOperationalQueuePage {

        @Test
        @DisplayName("agrupa por estágio de trabalho: IN_PROGRESS, WAITING_APPROVAL, DIAGNOSIS e RECEIVED")
        void ordersByWorkStage() {
            LocalDateTime openedAt = LocalDateTime.now();
            WorkOrder received = seed(WorkOrderPriority.URGENT, WorkOrderStatus.RECEIVED, null, openedAt);
            WorkOrder diagnosis = seed(WorkOrderPriority.LOW, WorkOrderStatus.DIAGNOSIS, null, openedAt);
            WorkOrder waitingApproval = seed(WorkOrderPriority.LOW, WorkOrderStatus.WAITING_APPROVAL, null, openedAt);
            WorkOrder inProgress = seed(WorkOrderPriority.LOW, WorkOrderStatus.IN_PROGRESS, null, openedAt);

            List<WorkOrder> page = inTransaction(() -> repository.findOperationalQueuePage(0, 1000));

            assertTrue(indexOf(page, inProgress.getId()) < indexOf(page, waitingApproval.getId()));
            assertTrue(indexOf(page, waitingApproval.getId()) < indexOf(page, diagnosis.getId()));
            assertTrue(indexOf(page, diagnosis.getId()) < indexOf(page, received.getId()));
        }

        @Test
        @DisplayName("coloca a ordem aberta há mais tempo à frente dentro do mesmo status")
        void ordersOldestFirstWithinTheSameStatus() {
            LocalDateTime now = LocalDateTime.now();
            WorkOrder newest = seed(WorkOrderPriority.URGENT, WorkOrderStatus.DIAGNOSIS, null, now);
            WorkOrder oldest = seed(WorkOrderPriority.LOW, WorkOrderStatus.DIAGNOSIS, null, now.minusDays(3));

            List<WorkOrder> page = inTransaction(() -> repository.findOperationalQueuePage(0, 1000));

            assertTrue(indexOf(page, oldest.getId()) < indexOf(page, newest.getId()));
        }

        @Test
        @DisplayName("exclui ordens concluídas e entregues, inclusive as concluídas por recusa")
        void excludesClosedWorkOrders() {
            WorkOrder completed = seed(WorkOrderPriority.MEDIUM, WorkOrderStatus.COMPLETED, LocalDateTime.now());
            WorkOrder delivered = seed(WorkOrderPriority.MEDIUM, WorkOrderStatus.DELIVERED, LocalDateTime.now());
            WorkOrder cancelled = seedCancelledByRejection();

            List<WorkOrder> page = inTransaction(() -> repository.findOperationalQueuePage(0, 1000));

            assertTrue(page.stream().noneMatch(order -> order.getId().equals(completed.getId())));
            assertTrue(page.stream().noneMatch(order -> order.getId().equals(delivered.getId())));
            assertTrue(page.stream().noneMatch(order -> order.getId().equals(cancelled.getId())));
            assertTrue(page.stream().allMatch(order -> WorkOrderStatus.operationalQueue().contains(order.getStatus())));
        }

        @Test
        @DisplayName("respeita o tamanho da página")
        void limitsThePageToTheRequestedSize() {
            seed(WorkOrderPriority.MEDIUM, WorkOrderStatus.RECEIVED, null);
            seed(WorkOrderPriority.MEDIUM, WorkOrderStatus.RECEIVED, null);

            List<WorkOrder> page = inTransaction(() -> repository.findOperationalQueuePage(0, 1));

            assertEquals(1, page.size());
        }

        @Test
        @DisplayName("não repete nem perde ordens abertas no mesmo instante ao paginar")
        void paginatesStablyWhenOpeningInstantsTie() {
            LocalDateTime sameInstant = LocalDateTime.now();
            seed(WorkOrderPriority.MEDIUM, WorkOrderStatus.RECEIVED, null, sameInstant);
            seed(WorkOrderPriority.MEDIUM, WorkOrderStatus.RECEIVED, null, sameInstant);
            seed(WorkOrderPriority.MEDIUM, WorkOrderStatus.RECEIVED, null, sameInstant);

            List<UUID> singlePage = pageIds(0, 1000);
            List<UUID> walkedInPairs = new ArrayList<>();
            for (int page = 0; page * 2 < singlePage.size(); page++) {
                walkedInPairs.addAll(pageIds(page, 2));
            }

            assertEquals(singlePage, walkedInPairs);
        }

        private List<UUID> pageIds(int page, int size) {
            return inTransaction(() -> repository.findOperationalQueuePage(page, size)).stream()
                    .map(WorkOrder::getId)
                    .toList();
        }

        // A recusa do orçamento conclui a OS e carimba cancelledAt; a fila precisa ignorá-la do
        // mesmo jeito que ignora uma conclusão normal.
        private WorkOrder seedCancelledByRejection() {
            WorkOrder cancelled = seed(WorkOrderPriority.MEDIUM, WorkOrderStatus.COMPLETED, LocalDateTime.now());
            return inTransaction(() -> repository.findByWorkOrderId(cancelled.getId())
                    .invoke(managed -> managed.setCancelledAt(LocalDateTime.now()))
                    .flatMap(repository::save));
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
    @DisplayName("countOperationalQueue")
    class CountOperationalQueue {

        @Test
        @DisplayName("cresce apenas com as ordens ainda em atendimento")
        void countsOnlyTheOperationalWorkOrders() {
            long before = inTransaction(() -> repository.countOperationalQueue());

            seed(WorkOrderPriority.MEDIUM, WorkOrderStatus.RECEIVED, null);
            seed(WorkOrderPriority.MEDIUM, WorkOrderStatus.COMPLETED, LocalDateTime.now());
            seed(WorkOrderPriority.MEDIUM, WorkOrderStatus.DELIVERED, LocalDateTime.now());

            assertEquals(before + 1, inTransaction(() -> repository.countOperationalQueue()));
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

    @Nested
    @DisplayName("saveWithHistory")
    class SaveWithHistory {

        @Test
        @DisplayName("persists the work order and its status transition together")
        void persistsOrderAndHistory() {
            WorkOrder workOrder = seed(WorkOrderPriority.MEDIUM, WorkOrderStatus.RECEIVED, null);

            inTransaction(() -> repository.findByWorkOrderId(workOrder.getId())
                    .flatMap(managed -> repository.saveWithHistory(
                            managed,
                            managed.transitionTo(WorkOrderStatus.DIAGNOSIS, false, LocalDateTime.now()))));

            WorkOrder updated = inTransaction(() -> repository.findByWorkOrderId(workOrder.getId()));
            long historyCount = inTransaction(() -> historyRepository.count("workOrder.id = ?1", workOrder.getId()));

            assertEquals(WorkOrderStatus.DIAGNOSIS, updated.getStatus());
            assertEquals(1L, historyCount);
        }
    }
}
