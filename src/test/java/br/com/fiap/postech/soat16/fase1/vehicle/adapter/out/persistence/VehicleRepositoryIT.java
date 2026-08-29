package br.com.fiap.postech.soat16.fase1.vehicle.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.customer.adapter.out.persistence.CustomerRepository;
import br.com.fiap.postech.soat16.fase1.customer.domain.model.Customer;
import br.com.fiap.postech.soat16.fase1.customer.domain.model.enums.DocumentType;
import br.com.fiap.postech.soat16.fase1.shared.test.infrastructure.PostgresTestResource;
import br.com.fiap.postech.soat16.fase1.vehicle.application.query.VehicleQuery;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.enums.VehicleType;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@DisplayName("Repositório de veículos — testes de integração")
class VehicleRepositoryIT {

    private static final AtomicInteger PLATE_COUNTER = new AtomicInteger();

    @Inject
    VehicleRepository repository;

    @Inject
    CustomerRepository customerRepository;

    @Nested
    @DisplayName("consultas diretas")
    class DirectQueries {

        @Test
        @DisplayName("encontra um veículo pela placa exata")
        void findsByExactPlate() {
            String plate = uniquePlate();
            Vehicle vehicle = seed(plate, "Fiat", "Uno");

            Vehicle found = inTransaction(() -> repository.findByLicensePlate(plate));

            assertEquals(vehicle.getId(), found.getId());
            assertEquals(plate, found.getLicensePlate());
        }

        @Test
        @DisplayName("retorna nulo quando a placa não existe")
        void returnsNullWhenPlateIsMissing() {
            assertNull(inTransaction(() -> repository.findByLicensePlate(uniquePlate())));
        }

        @Test
        @DisplayName("detecta uma placa já cadastrada")
        void detectsDuplicatePlate() {
            String plate = uniquePlate();
            seed(plate, "Fiat", "Uno");

            assertTrue(inTransaction(() -> repository.existsByLicensePlate(plate)));
            assertFalse(inTransaction(() -> repository.existsByLicensePlate(uniquePlate())));
        }

        @Test
        @DisplayName("encontra um veículo pelo identificador")
        void findsById() {
            Vehicle vehicle = seed(uniquePlate(), "Fiat", "Uno");

            Vehicle found =
                    inTransaction(() -> repository.findByVehicleId(vehicle.getId()));

            assertEquals(vehicle.getId(), found.getId());
        }

        @Test
        @DisplayName("retorna nulo quando o identificador não existe")
        void returnsNullWhenIdIsMissing() {
            assertNull(inTransaction(
                    () -> repository.findByVehicleId(UUID.randomUUID())));
        }

        @Test
        @DisplayName("identifica veículos associados ao cliente")
        void detectsCustomerAssociation() {
            Customer customer = seedCustomer();
            seed(customer, uniquePlate(), "Fiat", "Uno");

            assertTrue(inTransaction(
                    () -> repository.existsByCustomerId(customer.getId())));
            assertFalse(inTransaction(
                    () -> repository.existsByCustomerId(UUID.randomUUID())));
        }
    }

    @Nested
    @DisplayName("deleteByVehicleId")
    class DeleteByVehicleId {

        @Test
        @DisplayName("remove um veículo pelo identificador")
        void deletesById() {
            Vehicle vehicle = seed(uniquePlate(), "Fiat", "Uno");

            Long deleted =
                    inTransaction(() -> repository.deleteByVehicleId(vehicle.getId()));

            assertEquals(1L, deleted);
            assertNull(inTransaction(
                    () -> repository.findByVehicleId(vehicle.getId())));
        }

        @Test
        @DisplayName("retorna zero quando o identificador não existe")
        void returnsZeroWhenIdIsMissing() {
            Long deleted =
                    inTransaction(() -> repository.deleteByVehicleId(UUID.randomUUID()));

            assertEquals(0L, deleted);
        }
    }

    @Nested
    @DisplayName("findPageWithFilter")
    class FindPageWithFilter {

        @Test
        @DisplayName("sem filtros retorna os veículos persistidos")
        void returnsPersistedVehiclesWithoutFilters() {
            Vehicle first = seed(uniquePlate(), "Fiat", "Uno");
            Vehicle second = seed(uniquePlate(), "Honda", "Civic");

            List<Vehicle> page =
                    inTransaction(() -> repository.findPageWithFilter(query()));

            assertTrue(page.stream().anyMatch(
                    vehicle -> vehicle.getId().equals(first.getId())));
            assertTrue(page.stream().anyMatch(
                    vehicle -> vehicle.getId().equals(second.getId())));
        }

        @Test
        @DisplayName("filtra parcialmente pela placa sem diferenciar maiúsculas")
        void filtersByLicensePlate() {
            String plate = uniquePlate();
            seed(plate, "Fiat", "Uno");
            String partialPlate = plate.substring(2).toLowerCase(Locale.ROOT);

            List<Vehicle> page = inTransaction(() -> repository.findPageWithFilter(
                    query(partialPlate, null, null)));

            assertEquals(1, page.size());
            assertEquals(plate, page.getFirst().getLicensePlate());
        }

        @Test
        @DisplayName("filtra parcialmente pelo fabricante sem diferenciar maiúsculas")
        void filtersByManufacturer() {
            String plate = uniquePlate();
            String manufacturer = "Toyota-" + UUID.randomUUID();
            seed(plate, manufacturer, "Corolla");

            List<Vehicle> page = inTransaction(() -> repository.findPageWithFilter(
                    query(null, manufacturer.toLowerCase(Locale.ROOT), null)));

            assertEquals(1, page.size());
            assertEquals(plate, page.getFirst().getLicensePlate());
        }

        @Test
        @DisplayName("filtra parcialmente pelo modelo sem diferenciar maiúsculas")
        void filtersByModel() {
            String plate = uniquePlate();
            String model = "Modelo-" + UUID.randomUUID();
            seed(plate, "Fiat", model);

            List<Vehicle> page = inTransaction(() -> repository.findPageWithFilter(
                    query(null, null, model.toLowerCase(Locale.ROOT))));

            assertEquals(1, page.size());
            assertEquals(plate, page.getFirst().getLicensePlate());
        }

        @Test
        @DisplayName("retorna lista vazia quando nenhum veículo corresponde ao filtro")
        void returnsEmptyListWhenFilterDoesNotMatch() {
            seed(uniquePlate(), "Fiat", "Uno");

            List<Vehicle> page = inTransaction(() -> repository.findPageWithFilter(
                    query(null, "Inexistente-" + UUID.randomUUID(), null)));

            assertTrue(page.isEmpty());
        }

        @Test
        @DisplayName("ordena os resultados nas direções ascendente e descendente")
        void sortsInBothDirections() {
            String manufacturer = "Grupo-" + UUID.randomUUID();
            Vehicle first = seed(uniquePlate(), manufacturer, "Alfa");
            Vehicle second = seed(uniquePlate(), manufacturer, "Beta");
            VehicleQuery ascending = query(
                    0, 10, List.of("model,asc"), null, manufacturer, null);
            VehicleQuery descending = query(
                    0, 10, List.of("model,desc"), null, manufacturer, null);

            List<Vehicle> ascendingPage =
                    inTransaction(() -> repository.findPageWithFilter(ascending));
            List<Vehicle> descendingPage =
                    inTransaction(() -> repository.findPageWithFilter(descending));

            assertEquals(List.of(first.getId(), second.getId()),
                    ascendingPage.stream().map(Vehicle::getId).toList());
            assertEquals(List.of(second.getId(), first.getId()),
                    descendingPage.stream().map(Vehicle::getId).toList());
        }

        @Test
        @DisplayName("retorna lista vazia para uma página além dos resultados")
        void returnsEmptyListForOutOfRangePage() {
            String manufacturer = "Grupo-" + UUID.randomUUID();
            seed(uniquePlate(), manufacturer, "Uno");
            VehicleQuery query = query(
                    1_000, 10, List.of(), null, manufacturer, null);

            List<Vehicle> page =
                    inTransaction(() -> repository.findPageWithFilter(query));

            assertTrue(page.isEmpty());
        }
    }

    @Nested
    @DisplayName("countWithFilter")
    class CountWithFilter {

        @Test
        @DisplayName("conta os veículos que correspondem ao filtro")
        void countsFilteredVehicles() {
            String manufacturer = "Contagem-" + UUID.randomUUID();
            seed(uniquePlate(), manufacturer, "Uno");
            seed(uniquePlate(), manufacturer, "Palio");

            Long count = inTransaction(() -> repository.countWithFilter(
                    query(null, manufacturer.toLowerCase(Locale.ROOT), null)));

            assertEquals(2L, count);
        }

        @Test
        @DisplayName("sem filtros conta todos os veículos persistidos")
        void countsAllVehiclesWithoutFilters() {
            seed(uniquePlate(), "Fiat", "Uno");

            Long count = inTransaction(
                    () -> repository.countWithFilter(query()));

            assertTrue(count >= 1L);
        }
    }

    private VehicleQuery query() {
        return query(0, 100, List.of(), null, null, null);
    }

    private VehicleQuery query(
            String licensePlate, String manufacturer, String model) {
        return query(0, 100, List.of(), licensePlate, manufacturer, model);
    }

    private VehicleQuery query(
            int page,
            int size,
            List<String> sort,
            String licensePlate,
            String manufacturer,
            String model) {
        return VehicleQuery.of(
                page, size, sort, licensePlate, manufacturer, model);
    }

    private Customer seedCustomer() {
        Customer customer = new Customer();
        customer.setFirstName("Maria");
        customer.setLastName("Silva");
        customer.setPhoneNumber("11999999999");
        customer.setDocument("DOC-" + UUID.randomUUID());
        customer.setDocumentType(DocumentType.CPF);
        return inTransaction(() -> customerRepository.persist(customer));
    }

    private Vehicle seed(String licensePlate, String manufacturer, String model) {
        return seed(seedCustomer(), licensePlate, manufacturer, model);
    }

    private Vehicle seed(
            Customer customer, String licensePlate, String manufacturer, String model) {
        Vehicle vehicle = new Vehicle();
        vehicle.setCustomer(customer);
        vehicle.setLicensePlate(licensePlate);
        vehicle.setManufacturer(manufacturer);
        vehicle.setModel(model);
        vehicle.setColor("Branco");
        vehicle.setYear(2020);
        vehicle.setKmDriven(10_000L);
        vehicle.setType(VehicleType.CAR);
        return inTransaction(() -> repository.save(vehicle));
    }

    private static String uniquePlate() {
        return "VHC9A" + String.format("%02d", PLATE_COUNTER.incrementAndGet() % 100);
    }

    private <T> T inTransaction(Supplier<Uni<T>> action) {
        try {
            return VertxContextSupport.subscribeAndAwait(
                    () -> Panache.withTransaction(action));
        } catch (Throwable exception) {
            throw new IllegalStateException(
                    "Falha ao preparar dados de teste", exception);
        }
    }
}
