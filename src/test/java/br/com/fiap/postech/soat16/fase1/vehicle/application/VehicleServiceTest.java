package br.com.fiap.postech.soat16.fase1.vehicle.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.postech.soat16.fase1.customer.domain.exception.CustomerNotFoundException;
import br.com.fiap.postech.soat16.fase1.customer.domain.model.Customer;
import br.com.fiap.postech.soat16.fase1.vehicle.application.command.CreateVehicleCommand;
import br.com.fiap.postech.soat16.fase1.vehicle.application.command.UpdateVehicleCommand;
import br.com.fiap.postech.soat16.fase1.vehicle.application.port.out.VehicleCustomerLookupPort;
import br.com.fiap.postech.soat16.fase1.vehicle.application.port.out.VehiclePersistencePort;
import br.com.fiap.postech.soat16.fase1.vehicle.application.query.VehicleQuery;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.exception.DuplicateLicensePlateException;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.exception.VehicleNotFoundException;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.enums.VehicleType;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("Serviço de veículos — testes unitários")
class VehicleServiceTest {

    private static final UUID VEHICLE_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @Mock
    VehicleCustomerLookupPort customerLookup;

    @Mock
    VehiclePersistencePort vehiclePersistence;

    private VehicleService service;
    private Customer customer;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        service = new VehicleService(customerLookup, vehiclePersistence);
        customer = new Customer();
        customer.setId(CUSTOMER_ID);
        vehicle = new Vehicle(
                VEHICLE_ID,
                customer,
                "ABC1234",
                "Toyota",
                "Corolla",
                "Prata",
                2020,
                50_000L,
                VehicleType.CAR);
    }

    @Nested
    @DisplayName("listAll")
    class ListAll {

        @Test
        @DisplayName("retorna o conteúdo paginado por meio da porta de persistência")
        void returnsPagedContent() {
            VehicleQuery query = query(0, 10);
            when(vehiclePersistence.findPageWithFilter(query))
                    .thenReturn(Uni.createFrom().item(List.of(vehicle)));
            when(vehiclePersistence.countWithFilter(query))
                    .thenReturn(Uni.createFrom().item(1L));

            var result = service.listAll(query).await().indefinitely();

            assertEquals(1, result.content().size());
            assertEquals(VEHICLE_ID, result.content().getFirst().id());
            assertEquals("ABC1234", result.content().getFirst().licensePlate());
        }

        @Test
        @DisplayName("calcula os metadados da paginação")
        void returnsPaginationMetadata() {
            VehicleQuery query = query(0, 5);
            when(vehiclePersistence.findPageWithFilter(query))
                    .thenReturn(Uni.createFrom().item(List.of(vehicle)));
            when(vehiclePersistence.countWithFilter(query))
                    .thenReturn(Uni.createFrom().item(12L));

            var result = service.listAll(query).await().indefinitely();

            assertEquals(0, result.page());
            assertEquals(5, result.size());
            assertEquals(12L, result.totalElements());
            assertEquals(3, result.totalPages());
            assertFalse(result.hasPrevious());
            assertTrue(result.hasNext());
        }

        @Test
        @DisplayName("retorna página vazia quando nenhum veículo existe")
        void returnsEmptyPage() {
            VehicleQuery query = query(0, 10);
            when(vehiclePersistence.findPageWithFilter(query))
                    .thenReturn(Uni.createFrom().item(List.of()));
            when(vehiclePersistence.countWithFilter(query))
                    .thenReturn(Uni.createFrom().item(0L));

            var result = service.listAll(query).await().indefinitely();

            assertTrue(result.content().isEmpty());
            assertEquals(0L, result.totalElements());
            assertEquals(0, result.totalPages());
            assertFalse(result.hasPrevious());
            assertFalse(result.hasNext());
        }

        @Test
        @DisplayName("indica que a última página não possui próxima página")
        void marksLastPage() {
            VehicleQuery query = query(1, 10);
            when(vehiclePersistence.findPageWithFilter(query))
                    .thenReturn(Uni.createFrom().item(List.of(vehicle)));
            when(vehiclePersistence.countWithFilter(query))
                    .thenReturn(Uni.createFrom().item(11L));

            var result = service.listAll(query).await().indefinitely();

            assertTrue(result.hasPrevious());
            assertFalse(result.hasNext());
        }

        private VehicleQuery query(int page, int size) {
            return VehicleQuery.of(page, size, List.of(), null, null, null);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("retorna o resultado do veículo quando o identificador existe")
        void returnsVehicle() {
            when(vehiclePersistence.findByVehicleId(VEHICLE_ID))
                    .thenReturn(Uni.createFrom().item(vehicle));

            var result = service.findById(VEHICLE_ID).await().indefinitely();

            assertEquals(VEHICLE_ID, result.id());
            assertEquals("Toyota", result.manufacturer());
            verify(vehiclePersistence).findByVehicleId(VEHICLE_ID);
        }

        @Test
        @DisplayName("falha quando o identificador não existe")
        void failsWhenVehicleIsMissing() {
            UUID missingId = UUID.randomUUID();
            when(vehiclePersistence.findByVehicleId(missingId))
                    .thenReturn(Uni.createFrom().nullItem());

            assertThrows(
                    VehicleNotFoundException.class,
                    () -> service.findById(missingId).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("findByLicensePlate")
    class FindByLicensePlate {

        @Test
        @DisplayName("normaliza a placa e retorna o veículo encontrado")
        void normalizesPlateAndReturnsVehicle() {
            when(vehiclePersistence.findByLicensePlate("ABC1234"))
                    .thenReturn(Uni.createFrom().item(vehicle));

            var result = service.findByLicensePlate("abc-1234").await().indefinitely();

            assertEquals(VEHICLE_ID, result.id());
            assertEquals("ABC1234", result.licensePlate());
            verify(vehiclePersistence).findByLicensePlate("ABC1234");
        }

        @Test
        @DisplayName("falha quando a placa normalizada não existe")
        void failsWhenPlateIsMissing() {
            when(vehiclePersistence.findByLicensePlate("XYZ9999"))
                    .thenReturn(Uni.createFrom().nullItem());

            assertThrows(
                    VehicleNotFoundException.class,
                    () -> service.findByLicensePlate("xyz-9999").await().indefinitely());
            verify(vehiclePersistence).findByLicensePlate("XYZ9999");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("resolve o cliente e persiste o veículo por meio das portas")
        void createsVehicle() {
            CreateVehicleCommand command = createCommand();
            when(customerLookup.findByCustomerId(CUSTOMER_ID))
                    .thenReturn(Uni.createFrom().item(customer));
            when(vehiclePersistence.existsByLicensePlate("ABC1234"))
                    .thenReturn(Uni.createFrom().item(false));
            when(vehiclePersistence.save(any(Vehicle.class)))
                    .thenReturn(Uni.createFrom().item(vehicle));

            assertDoesNotThrow(() -> service.create(command).await().indefinitely());

            verify(customerLookup).findByCustomerId(CUSTOMER_ID);
            verify(vehiclePersistence).save(any(Vehicle.class));
        }

        @Test
        @DisplayName("falha quando o cliente não existe")
        void failsWhenCustomerIsMissing() {
            CreateVehicleCommand command = createCommand();
            when(customerLookup.findByCustomerId(CUSTOMER_ID))
                    .thenReturn(Uni.createFrom().nullItem());

            assertThrows(
                    CustomerNotFoundException.class,
                    () -> service.create(command).await().indefinitely());
            verify(vehiclePersistence, never()).save(any(Vehicle.class));
        }

        @Test
        @DisplayName("rejeita uma placa já cadastrada")
        void rejectsDuplicatePlate() {
            CreateVehicleCommand command = createCommand();
            when(customerLookup.findByCustomerId(CUSTOMER_ID))
                    .thenReturn(Uni.createFrom().item(customer));
            when(vehiclePersistence.existsByLicensePlate("ABC1234"))
                    .thenReturn(Uni.createFrom().item(true));

            assertThrows(
                    DuplicateLicensePlateException.class,
                    () -> service.create(command).await().indefinitely());
            verify(vehiclePersistence, never()).save(any(Vehicle.class));
        }

        private CreateVehicleCommand createCommand() {
            return new CreateVehicleCommand(
                    CUSTOMER_ID,
                    "ABC1234",
                    "Toyota",
                    "Corolla",
                    "Prata",
                    2020,
                    50_000L,
                    VehicleType.CAR);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("atualiza os dados sem alterar o cliente")
        void updatesVehicle() {
            UpdateVehicleCommand command = updateCommand(VEHICLE_ID);
            when(vehiclePersistence.findByVehicleId(VEHICLE_ID))
                    .thenReturn(Uni.createFrom().item(vehicle));
            when(vehiclePersistence.save(vehicle))
                    .thenReturn(Uni.createFrom().item(vehicle));

            var result = service.update(command).await().indefinitely();

            assertEquals(VEHICLE_ID, result.id());
            assertEquals("XYZ9876", result.licensePlate());
            assertEquals("Honda", result.manufacturer());
            assertEquals("Civic", result.model());
            assertEquals("Preto", result.color());
            assertEquals(2022, result.year());
            assertEquals(10_000L, result.kmDriven());
            assertEquals(VehicleType.MOTORCYCLE, result.type());
            assertEquals(CUSTOMER_ID, result.customerId());
            verify(vehiclePersistence).save(vehicle);
        }

        @Test
        @DisplayName("falha sem persistir quando o veículo não existe")
        void failsWhenVehicleIsMissing() {
            UUID missingId = UUID.randomUUID();
            UpdateVehicleCommand command = updateCommand(missingId);
            when(vehiclePersistence.findByVehicleId(missingId))
                    .thenReturn(Uni.createFrom().nullItem());

            assertThrows(
                    VehicleNotFoundException.class,
                    () -> service.update(command).await().indefinitely());
            verify(vehiclePersistence, never()).save(any(Vehicle.class));
        }

        private UpdateVehicleCommand updateCommand(UUID id) {
            return new UpdateVehicleCommand(
                    id,
                    "XYZ9876",
                    "Honda",
                    "Civic",
                    "Preto",
                    2022,
                    10_000L,
                    VehicleType.MOTORCYCLE);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("remove o veículo quando o identificador existe")
        void deletesVehicle() {
            when(vehiclePersistence.deleteByVehicleId(VEHICLE_ID))
                    .thenReturn(Uni.createFrom().item(1L));

            assertDoesNotThrow(() -> service.delete(VEHICLE_ID).await().indefinitely());

            verify(vehiclePersistence).deleteByVehicleId(VEHICLE_ID);
        }

        @Test
        @DisplayName("falha quando nenhum veículo é removido")
        void failsWhenVehicleIsMissing() {
            when(vehiclePersistence.deleteByVehicleId(VEHICLE_ID))
                    .thenReturn(Uni.createFrom().item(0L));

            assertThrows(
                    VehicleNotFoundException.class,
                    () -> service.delete(VEHICLE_ID).await().indefinitely());
        }
    }

}
