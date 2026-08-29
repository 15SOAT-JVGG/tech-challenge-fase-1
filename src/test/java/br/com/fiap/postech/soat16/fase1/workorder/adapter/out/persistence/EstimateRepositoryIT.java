package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
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
import br.com.fiap.postech.soat16.fase1.part.adapter.out.persistence.PartRepository;
import br.com.fiap.postech.soat16.fase1.part.domain.model.Part;
import br.com.fiap.postech.soat16.fase1.shared.test.infrastructure.PostgresTestResource;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.out.persistence.VehicleRepository;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.enums.VehicleType;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.Estimate;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.EstimateItem;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrder;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.EstimateStatus;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderPriority;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderStatus;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@DisplayName("EstimateRepository — Integration Tests")
class EstimateRepositoryIT {

    @Inject
    EstimateRepository repository;

    @Inject
    CustomerRepository customerRepository;

    @Inject
    VehicleRepository vehicleRepository;

    @Inject
    WorkOrderRepository workOrderRepository;

    @Inject
    PartRepository partRepository;

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

        return inTransaction(() -> customerRepository.save(customer)
            .chain(c -> vehicleRepository.save(vehicle)
                .chain(v -> {
                    WorkOrder workOrder = new WorkOrder();
                    workOrder.setCustomer(c);
                    workOrder.setVehicle(v);
                    workOrder.setDescription("Revisao geral");
                    workOrder.setPriority(WorkOrderPriority.MEDIUM);
                    workOrder.setStatus(WorkOrderStatus.WAITING_APPROVAL);
                    workOrder.setOpenedAt(LocalDateTime.now());
                    return workOrderRepository.save(workOrder);
                })));
    }

    private static final java.util.concurrent.atomic.AtomicInteger PLATE_COUNTER = new java.util.concurrent.atomic.AtomicInteger();

    private static String uniquePlate() {
        return "EST9A" + String.format("%02d", PLATE_COUNTER.incrementAndGet() % 100);
    }

    private Part seedPart() {
        Part part = new Part("Filtro", "desc", new BigDecimal("50.00"), 100, "UN");
        return inTransaction(() -> partRepository.save(part));
    }

    private Estimate seedEstimate(WorkOrder workOrder, Part part, EstimateStatus status) {
        Estimate estimate = new Estimate();
        estimate.setWorkOrder(workOrder);
        estimate.setStatus(status);
        estimate.setPartsAmount(new BigDecimal("50.00"));
        estimate.setLaborAmount(BigDecimal.ZERO);
        estimate.setTotalAmount(new BigDecimal("50.00"));

        EstimateItem item = new EstimateItem();
        item.setEstimate(estimate);
        item.setPart(part);
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("50.00"));
        item.setTotalPrice(new BigDecimal("50.00"));
        estimate.setItems(java.util.List.of(item));

        return inTransaction(() -> repository.save(estimate));
    }

    @Nested
    @DisplayName("findByEstimateIdAndWorkOrderId")
    class FindByEstimateIdAndWorkOrderId {

        @Test
        @DisplayName("finds the estimate with its items and parts eagerly fetched")
        void findsEstimateWithItemsFetched() {
            WorkOrder workOrder = seedWorkOrder();
            Part part = seedPart();
            Estimate estimate = seedEstimate(workOrder, part, EstimateStatus.PENDING);

            Estimate found = inTransaction(
                () -> repository.findByEstimateIdAndWorkOrderId(estimate.getId(), workOrder.getId()));

            assertEquals(estimate.getId(), found.getId());
            assertEquals(1, found.getItems().size());
            assertEquals(part.getId(), found.getItems().get(0).getPart().getId());
        }

        @Test
        @DisplayName("returns null when the estimate does not belong to the given work order")
        void returnsNullWhenWorkOrderMismatches() {
            WorkOrder workOrder = seedWorkOrder();
            WorkOrder otherWorkOrder = seedWorkOrder();
            Part part = seedPart();
            Estimate estimate = seedEstimate(workOrder, part, EstimateStatus.PENDING);

            Estimate found = inTransaction(
                () -> repository.findByEstimateIdAndWorkOrderId(estimate.getId(), otherWorkOrder.getId()));

            assertNull(found);
        }
    }

    @Nested
    @DisplayName("findApprovedByWorkOrderId / existsApprovedByWorkOrderId")
    class FindApprovedByWorkOrderId {

        @Test
        @DisplayName("finds the approved estimate for the work order")
        void findsApprovedEstimate() {
            WorkOrder workOrder = seedWorkOrder();
            Part part = seedPart();
            Estimate approved = seedEstimate(workOrder, part, EstimateStatus.APPROVED);

            Estimate found = inTransaction(() -> repository.findApprovedByWorkOrderId(workOrder.getId()));

            assertEquals(approved.getId(), found.getId());
            assertTrue(inTransaction(() -> repository.existsApprovedByWorkOrderId(workOrder.getId())));
        }

        @Test
        @DisplayName("returns null/false when the work order has only a pending estimate")
        void returnsNullWhenOnlyPendingEstimateExists() {
            WorkOrder workOrder = seedWorkOrder();
            Part part = seedPart();
            seedEstimate(workOrder, part, EstimateStatus.PENDING);

            assertNull(inTransaction(() -> repository.findApprovedByWorkOrderId(workOrder.getId())));
            assertFalse(inTransaction(() -> repository.existsApprovedByWorkOrderId(workOrder.getId())));
        }
    }
}
