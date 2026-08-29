package br.com.fiap.postech.soat16.fase1.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.request.VehicleFilterDto;
import br.com.fiap.postech.soat16.fase1.dto.request.VehicleRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.VehicleResponseDto;
import br.com.fiap.postech.soat16.fase1.exception.DuplicateLicensePlateException;
import br.com.fiap.postech.soat16.fase1.exception.VehicleNotFoundException;
import br.com.fiap.postech.soat16.fase1.mapper.VehicleMapper;
import br.com.fiap.postech.soat16.fase1.model.Customer;
import br.com.fiap.postech.soat16.fase1.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.model.enums.VehicleType;
import br.com.fiap.postech.soat16.fase1.repository.CustomerRepository;
import br.com.fiap.postech.soat16.fase1.repository.VehicleRepository;
import br.com.fiap.postech.soat16.fase1.service.query.VehicleQuery;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleService — Unit Tests")
class VehicleServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleMapper vehicleMapper;

    private VehicleService service;

    private static final UUID VEHICLE_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    private Customer customer;
    private Vehicle entity;
    private VehicleResponseDto response;
    private VehicleRequestDto request;

    @BeforeEach
    void setUp() {
        service = new VehicleService(customerRepository, vehicleRepository, vehicleMapper);
        customer = new Customer();
        customer.setId(CUSTOMER_ID);
        entity = new Vehicle(VEHICLE_ID, customer, "ABC1234", "Toyota", "Corolla", "Prata", 2020, 50000L, VehicleType.CAR);
        response = new VehicleResponseDto(VEHICLE_ID, "ABC1234", "Toyota", "Corolla", "Prata", 2020, 50000L,
                VehicleType.CAR, CUSTOMER_ID, null);
        request = new VehicleRequestDto(CUSTOMER_ID, "ABC1234", "Toyota", "Corolla", "Prata", 2020, 50000L, VehicleType.CAR);
    }

    @Nested
    @DisplayName("listAll")
    class ListAll {

        @Test
        @DisplayName("should return paginated response with correct content")
        void shouldReturnPaginatedResponse() {
            PageableRequestDto pageable = mock(PageableRequestDto.class);
            VehicleFilterDto filter = mock(VehicleFilterDto.class);
            when(pageable.getPage()).thenReturn(0);
            when(pageable.getSize()).thenReturn(10);
            when(vehicleRepository.findPageWithFilter(any(VehicleQuery.class))).thenReturn(Uni.createFrom().item(List.of(entity)));
            when(vehicleRepository.countWithFilter(any(VehicleQuery.class))).thenReturn(Uni.createFrom().item(1L));
            when(vehicleMapper.toResponse(entity)).thenReturn(response);

            PageableResponseDto<VehicleResponseDto> result = service.listAll(pageable, filter).await().indefinitely();

            assertNotNull(result);
            assertEquals(1, result.content().size());
            assertEquals("ABC1234", result.content().getFirst().licensePlate());
            assertEquals("Toyota", result.content().getFirst().manufacturer());
        }

        @Test
        @DisplayName("should return correct pagination metadata")
        void shouldReturnCorrectPaginationMetadata() {
            PageableRequestDto pageable = mock(PageableRequestDto.class);
            VehicleFilterDto filter = mock(VehicleFilterDto.class);
            when(pageable.getPage()).thenReturn(0);
            when(pageable.getSize()).thenReturn(5);
            when(vehicleRepository.findPageWithFilter(any(VehicleQuery.class))).thenReturn(Uni.createFrom().item(List.of(entity)));
            when(vehicleRepository.countWithFilter(any(VehicleQuery.class))).thenReturn(Uni.createFrom().item(12L));
            when(vehicleMapper.toResponse(entity)).thenReturn(response);

            PageableResponseDto<VehicleResponseDto> result = service.listAll(pageable, filter).await().indefinitely();

            assertEquals(0, result.pagination().page());
            assertEquals(5, result.pagination().size());
            assertEquals(12L, result.pagination().totalElements());
            assertEquals(3, result.pagination().totalPages());
            assertFalse(result.pagination().hasPrevious());
            assertTrue(result.pagination().hasNext());
        }

        @Test
        @DisplayName("should return empty page when no vehicles exist")
        void shouldReturnEmptyPage() {
            PageableRequestDto pageable = mock(PageableRequestDto.class);
            VehicleFilterDto filter = mock(VehicleFilterDto.class);
            when(pageable.getPage()).thenReturn(0);
            when(pageable.getSize()).thenReturn(10);
            when(vehicleRepository.findPageWithFilter(any(VehicleQuery.class))).thenReturn(Uni.createFrom().item(List.of()));
            when(vehicleRepository.countWithFilter(any(VehicleQuery.class))).thenReturn(Uni.createFrom().item(0L));

            PageableResponseDto<VehicleResponseDto> result = service.listAll(pageable, filter).await().indefinitely();

            assertTrue(result.content().isEmpty());
            assertEquals(0L, result.pagination().totalElements());
        }

        @Test
        @DisplayName("last page should not have next")
        void lastPageShouldNotHaveNext() {
            PageableRequestDto pageable = mock(PageableRequestDto.class);
            VehicleFilterDto filter = mock(VehicleFilterDto.class);
            when(pageable.getPage()).thenReturn(1);
            when(pageable.getSize()).thenReturn(10);
            when(vehicleRepository.findPageWithFilter(any(VehicleQuery.class))).thenReturn(Uni.createFrom().item(List.of(entity)));
            when(vehicleRepository.countWithFilter(any(VehicleQuery.class))).thenReturn(Uni.createFrom().item(11L));
            when(vehicleMapper.toResponse(entity)).thenReturn(response);

            PageableResponseDto<VehicleResponseDto> result = service.listAll(pageable, filter).await().indefinitely();

            assertTrue(result.pagination().hasPrevious());
            assertFalse(result.pagination().hasNext());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return vehicle response when found")
        void shouldReturnVehicleWhenFound() {
            when(vehicleRepository.findByVehicleId(VEHICLE_ID)).thenReturn(Uni.createFrom().item(entity));
            when(vehicleMapper.toResponse(entity)).thenReturn(response);

            VehicleResponseDto result = service.findById(VEHICLE_ID).await().indefinitely();

            assertNotNull(result);
            assertEquals(VEHICLE_ID, result.id());
            assertEquals("ABC1234", result.licensePlate());
            assertEquals("Toyota", result.manufacturer());
            verify(vehicleRepository).findByVehicleId(VEHICLE_ID);
        }

        @Test
        @DisplayName("should throw VehicleNotFoundException when not found")
        void shouldThrowNotFoundWhenMissing() {
            UUID missingId = UUID.randomUUID();
            when(vehicleRepository.findByVehicleId(missingId)).thenReturn(Uni.createFrom().nullItem());

            assertThrows(VehicleNotFoundException.class,
                    () -> service.findById(missingId).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("findByLicensePlate")
    class FindByLicensePlate {

        @Test
        @DisplayName("should return vehicle response when found")
        void shouldReturnVehicleWhenFound() {
            when(vehicleRepository.findByLicensePlate("ABC1234")).thenReturn(Uni.createFrom().item(entity));
            when(vehicleMapper.toResponse(entity)).thenReturn(response);

            VehicleResponseDto result = service.findByLicensePlate("ABC1234").await().indefinitely();

            assertNotNull(result);
            assertEquals("ABC1234", result.licensePlate());
            verify(vehicleRepository).findByLicensePlate("ABC1234");
        }

        @Test
        @DisplayName("should throw VehicleNotFoundException when license plate not found")
        void shouldThrowNotFoundWhenMissing() {
            when(vehicleRepository.findByLicensePlate("XYZ9999")).thenReturn(Uni.createFrom().nullItem());

            assertThrows(VehicleNotFoundException.class,
                    () -> service.findByLicensePlate("XYZ9999").await().indefinitely());
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should persist entity when license plate is unique")
        void shouldPersistWhenPlateIsUnique() {
            when(customerRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Uni.createFrom().item(customer));
            when(vehicleRepository.existsByLicensePlate("ABC1234")).thenReturn(Uni.createFrom().item(false));
            when(vehicleMapper.toEntity(request, customer)).thenReturn(entity);
            when(vehicleRepository.persist(entity)).thenReturn(Uni.createFrom().item(entity));

            assertDoesNotThrow(() -> service.create(request).await().indefinitely());
            verify(vehicleRepository).persist(entity);
        }

        @Test
        @DisplayName("should throw DuplicateLicensePlateException when plate already exists")
        void shouldThrowDuplicateWhenPlateExists() {
            when(customerRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Uni.createFrom().item(customer));
            when(vehicleRepository.existsByLicensePlate("ABC1234")).thenReturn(Uni.createFrom().item(true));

            assertThrows(DuplicateLicensePlateException.class,
                    () -> service.create(request).await().indefinitely());
            verify(vehicleRepository, never()).persist(any(Vehicle.class));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update entity and return response with correct data")
        void shouldUpdateAndReturn() {
            when(vehicleRepository.findByVehicleId(VEHICLE_ID)).thenReturn(Uni.createFrom().item(entity));
            when(vehicleRepository.persist(entity)).thenReturn(Uni.createFrom().item(entity));
            when(vehicleMapper.toResponse(entity)).thenReturn(response);

            VehicleResponseDto result = service.update(VEHICLE_ID, request).await().indefinitely();

            assertNotNull(result);
            assertEquals(VEHICLE_ID, result.id());
            assertEquals("ABC1234", result.licensePlate());
            verify(vehicleMapper).updateEntity(entity, request);
            verify(vehicleRepository).persist(entity);
        }

        @Test
        @DisplayName("should throw VehicleNotFoundException when entity not found")
        void shouldThrowNotFoundWhenMissing() {
            UUID missingId = UUID.randomUUID();
            when(vehicleRepository.findByVehicleId(missingId)).thenReturn(Uni.createFrom().nullItem());

            assertThrows(VehicleNotFoundException.class,
                    () -> service.update(missingId, request).await().indefinitely());
            verify(vehicleRepository, never()).persist(any(Vehicle.class));
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete vehicle when found")
        void shouldDeleteWhenFound() {
            when(vehicleRepository.deleteByVehicleId(VEHICLE_ID)).thenReturn(Uni.createFrom().item(1L));

            assertDoesNotThrow(() -> service.delete(VEHICLE_ID).await().indefinitely());
            verify(vehicleRepository).deleteByVehicleId(VEHICLE_ID);
        }

        @Test
        @DisplayName("should throw VehicleNotFoundException when vehicle not found")
        void shouldThrowWhenNotFound() {
            UUID missingId = UUID.randomUUID();
            when(vehicleRepository.deleteByVehicleId(missingId)).thenReturn(Uni.createFrom().item(0L));

            assertThrows(VehicleNotFoundException.class,
                    () -> service.delete(missingId).await().indefinitely());
        }
    }
}
