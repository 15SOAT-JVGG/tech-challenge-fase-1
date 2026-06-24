package br.com.fiap.postech.soat16.fase1.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.VehicleFilterDto;
import br.com.fiap.postech.soat16.fase1.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.model.enums.VehicleType;
import br.com.fiap.postech.soat16.fase1.security.PostgresTestResource;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@DisplayName("VehicleRepository — Integration Tests")
class VehicleRepositoryIT {

    @Inject
    VehicleRepository repository;

    private <T> T inTransaction(Supplier<Uni<T>> action) {
        try {
            return VertxContextSupport.subscribeAndAwait(() -> Panache.withTransaction(action));
        } catch (Throwable e) {
            throw new IllegalStateException("Falha ao preparar dados de teste", e);
        }
    }

    private Vehicle seed(String licensePlate, String manufacturer, String model) {
        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(licensePlate);
        vehicle.setManufacturer(manufacturer);
        vehicle.setModel(model);
        vehicle.setColor("Branco");
        vehicle.setYear(2020);
        vehicle.setKmDriven(10_000L);
        vehicle.setType(VehicleType.CAR);
        return inTransaction(() -> repository.persist(vehicle));
    }

    private static final java.util.concurrent.atomic.AtomicInteger PLATE_COUNTER = new java.util.concurrent.atomic.AtomicInteger();

    // Matches Vehicle's license plate pattern (^[A-Z]{3}\d[A-Z\d]\d{2}$), e.g. "VHC9A01".
    private static String uniquePlate() {
        return "VHC9A" + String.format("%02d", PLATE_COUNTER.incrementAndGet() % 100);
    }

    private VehicleFilterDto filterBy(String manufacturer) throws ReflectiveOperationException {
        VehicleFilterDto filter = new VehicleFilterDto();
        Field field = VehicleFilterDto.class.getDeclaredField("manufacturer");
        field.setAccessible(true);
        field.set(filter, manufacturer);
        return filter;
    }

    private PageableRequestDto pageable(int page, int size) throws ReflectiveOperationException {
        PageableRequestDto pageable = new PageableRequestDto();
        setField(pageable, "page", page);
        setField(pageable, "size", size);
        return pageable;
    }

    private void setField(Object target, String name, Object value) throws ReflectiveOperationException {
        Field field = PageableRequestDto.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Nested
    @DisplayName("findByLicensePlate / existsByLicensePlate")
    class FindByLicensePlate {

        @Test
        @DisplayName("finds a vehicle by its exact license plate")
        void findsByExactPlate() {
            String plate = uniquePlate();
            seed(plate, "Fiat", "Uno");

            Vehicle found = inTransaction(() -> repository.findByLicensePlate(plate));

            assertEquals(plate, found.getLicensePlate());
        }

        @Test
        @DisplayName("returns null when there is no vehicle with the given plate")
        void returnsNullWhenNotFound() {
            assertNull(inTransaction(() -> repository.findByLicensePlate(uniquePlate())));
        }

        @Test
        @DisplayName("existsByLicensePlate distinguishes a persisted plate from an unknown one")
        void existsByLicensePlate() {
            String plate = uniquePlate();
            seed(plate, "Fiat", "Uno");

            assertTrue(inTransaction(() -> repository.existsByLicensePlate(plate)));
            assertFalse(inTransaction(() -> repository.existsByLicensePlate(uniquePlate())));
        }
    }

    @Nested
    @DisplayName("findPageWithFilter / countWithFilter")
    class FindPageWithFilter {

        @Test
        @DisplayName("without filters, returns all persisted vehicles (paginated)")
        void noFilterReturnsAllVehicles() throws ReflectiveOperationException {
            seed(uniquePlate(), "Fiat", "Uno");
            seed(uniquePlate(), "Honda", "Civic");
            PageableRequestDto page0 = pageable(0, 50);

            List<Vehicle> page = inTransaction(() -> repository.findPageWithFilter(page0, new VehicleFilterDto()));

            assertTrue(page.size() >= 2);
        }

        @Test
        @DisplayName("filters by manufacturer (case-insensitive, partial match)")
        void filtersByManufacturer() throws ReflectiveOperationException {
            String plate = uniquePlate();
            seed(plate, "Toyota-" + plate, "Corolla");
            seed(uniquePlate(), "Honda", "Civic");
            VehicleFilterDto filter = filterBy("toyota-" + plate.toLowerCase(java.util.Locale.ROOT));
            PageableRequestDto page0 = pageable(0, 50);

            List<Vehicle> page = inTransaction(() -> repository.findPageWithFilter(page0, filter));

            assertEquals(1, page.size());
            assertEquals(plate, page.get(0).getLicensePlate());
        }

        @Test
        @DisplayName("returns an empty list when no vehicle matches the filter")
        void returnsEmptyListWhenFilterMatchesNothing() throws ReflectiveOperationException {
            seed(uniquePlate(), "Fiat", "Uno");
            VehicleFilterDto filter = filterBy("Unobtainium-" + UUID.randomUUID());
            PageableRequestDto page0 = pageable(0, 50);

            List<Vehicle> page = inTransaction(() -> repository.findPageWithFilter(page0, filter));

            assertTrue(page.isEmpty());
        }

        @Test
        @DisplayName("countWithFilter matches the number of vehicles returned by the same filter")
        void countWithFilterMatchesFilteredResults() throws ReflectiveOperationException {
            String plate = uniquePlate();
            seed(plate, "Yamaha-" + plate, "Fazer");
            VehicleFilterDto filter = filterBy("yamaha-" + plate.toLowerCase(java.util.Locale.ROOT));

            Long count = inTransaction(() -> repository.countWithFilter(filter));

            assertEquals(1L, count);
        }

        @Test
        @DisplayName("countWithFilter without filters counts all persisted vehicles")
        void countWithoutFilterCountsAll() {
            seed(uniquePlate(), "Fiat", "Uno");

            Long count = inTransaction(() -> repository.countWithFilter(new VehicleFilterDto()));

            assertTrue(count >= 1);
        }
    }
}
