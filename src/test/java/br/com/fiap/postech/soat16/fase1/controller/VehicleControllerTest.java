package br.com.fiap.postech.soat16.fase1.controller;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.pagination.PaginationDto;
import br.com.fiap.postech.soat16.fase1.dto.request.VehicleFilterDto;
import br.com.fiap.postech.soat16.fase1.dto.request.VehicleDto;
import br.com.fiap.postech.soat16.fase1.dto.response.VehicleResponseDto;
import br.com.fiap.postech.soat16.fase1.exception.DuplicateLicensePlateException;
import br.com.fiap.postech.soat16.fase1.exception.ResourceNotFoundException;
import br.com.fiap.postech.soat16.fase1.model.VehicleType;
import br.com.fiap.postech.soat16.fase1.service.VehicleService;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleController — Unit Tests")
class VehicleControllerTest {

    @Mock
    private VehicleService vehicleService;

    private VehicleController controller;

    private VehicleResponseDto response;
    private VehicleDto request;

    @BeforeEach
    void setUp() {
        controller = new VehicleController(vehicleService);
        response = new VehicleResponseDto(1L, "ABC1234", "Toyota", "Corolla", "Prata", 2020, 50000L, VehicleType.CARRO, null);
        request = new VehicleDto(null, "ABC1234", "Toyota", "Corolla", "Prata", 2020, 50000L, VehicleType.CARRO);
    }

    @Nested
    @DisplayName("GET /v1/vehicle — listAll")
    class ListAll {

        @Test
        @DisplayName("should return paginated list when vehicles exist")
        void shouldReturnPaginatedListWhenVehiclesExist() {
            PageableRequestDto pageable = mock(PageableRequestDto.class);
            VehicleFilterDto filter = mock(VehicleFilterDto.class);

            PaginationDto paginationDto = new PaginationDto(0, 10, 1L, 1, false, false);
            PageableResponseDto<VehicleResponseDto> page = new PageableResponseDto<>(List.of(response), paginationDto);
            when(vehicleService.listAll(pageable, filter)).thenReturn(Uni.createFrom().item(page));

            PageableResponseDto<VehicleResponseDto> result = controller.listAll(pageable, filter).await().indefinitely();

            assertNotNull(result);
            assertEquals(1, result.content().size());
            assertEquals("ABC1234", result.content().get(0).licensePlate());
            verify(vehicleService).listAll(pageable, filter);
        }

        @Test
        @DisplayName("should return empty page when no vehicles exist")
        void shouldReturnEmptyPageWhenNoVehiclesExist() {
            PageableRequestDto pageable = mock(PageableRequestDto.class);
            VehicleFilterDto filter = mock(VehicleFilterDto.class);

            when(vehicleService.listAll(pageable, filter)).thenReturn(Uni.createFrom().item(PageableResponseDto.emptyList()));

            PageableResponseDto<VehicleResponseDto> result = controller.listAll(pageable, filter).await().indefinitely();

            assertTrue(result.content().isEmpty());
        }

        @Test
        @DisplayName("should propagate exception from service")
        void shouldPropagateException() {
            PageableRequestDto pageable = mock(PageableRequestDto.class);
            VehicleFilterDto filter = mock(VehicleFilterDto.class);

            when(vehicleService.listAll(pageable, filter))
                    .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

            assertThrows(RuntimeException.class, () -> controller.listAll(pageable, filter).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("GET /v1/vehicle/{id} — findById")
    class FindById {

        @Test
        @DisplayName("should return vehicle when found")
        void shouldReturnVehicleWhenFound() {
            when(vehicleService.findById(1L)).thenReturn(Uni.createFrom().item(response));

            VehicleResponseDto result = controller.findById(1L).await().indefinitely();

            assertNotNull(result);
            assertEquals("ABC1234", result.licensePlate());
            verify(vehicleService).findById(1L);
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException")
        void shouldPropagateNotFoundException() {
            when(vehicleService.findById(99L))
                    .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Vehicle not found")));

            assertThrows(ResourceNotFoundException.class,
                    () -> controller.findById(99L).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("GET /v1/vehicle/license-plate/{license_plate} — findByLicensePlate")
    class FindByLicensePlate {

        @Test
        @DisplayName("should return vehicle when license plate is found")
        void shouldReturnVehicleWhenFound() {
            when(vehicleService.findByLicensePlate("ABC1234")).thenReturn(Uni.createFrom().item(response));

            VehicleResponseDto result = controller.findByLicensePlate("ABC1234").await().indefinitely();

            assertNotNull(result);
            assertEquals("ABC1234", result.licensePlate());
            assertEquals("Toyota", result.manufacturer());
            verify(vehicleService).findByLicensePlate("ABC1234");
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException when license plate not found")
        void shouldPropagateNotFoundException() {
            when(vehicleService.findByLicensePlate("XYZ9999"))
                    .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Vehicle not found")));

            assertThrows(ResourceNotFoundException.class,
                    () -> controller.findByLicensePlate("XYZ9999").await().indefinitely());
        }
    }

    @Nested
    @DisplayName("POST /v1/vehicle — create")
    class Create {

        @Test
        @DisplayName("should return HTTP 201 when create succeeds")
        void shouldReturn201WhenCreateSucceeds() {
            when(vehicleService.create(request)).thenReturn(Uni.createFrom().voidItem());

            Response result = controller.create(request).await().indefinitely();

            assertEquals(201, result.getStatus());
            verify(vehicleService).create(request);
        }

        @Test
        @DisplayName("should propagate DuplicateLicensePlateException")
        void shouldPropagateDuplicatePlateException() {
            when(vehicleService.create(request))
                    .thenReturn(Uni.createFrom().failure(new DuplicateLicensePlateException()));

            assertThrows(DuplicateLicensePlateException.class,
                    () -> controller.create(request).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("PUT /v1/vehicle/{id} — update")
    class Update {

        @Test
        @DisplayName("should return HTTP 200 with updated body")
        void shouldReturn200WithUpdatedBody() {
            when(vehicleService.update(1L, request)).thenReturn(Uni.createFrom().item(response));

            Response result = controller.update(1L, request).await().indefinitely();

            assertEquals(200, result.getStatus());
            verify(vehicleService).update(1L, request);
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException")
        void shouldPropagateNotFoundException() {
            when(vehicleService.update(99L, request))
                    .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Vehicle not found")));

            assertThrows(ResourceNotFoundException.class,
                    () -> controller.update(99L, request).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("DELETE /v1/vehicle/{id} — delete")
    class Delete {

        @Test
        @DisplayName("should return HTTP 204 when delete succeeds")
        void shouldReturn204WhenDeleteSucceeds() {
            when(vehicleService.delete(1L)).thenReturn(Uni.createFrom().voidItem());

            Response result = controller.delete(1L).await().indefinitely();

            assertEquals(204, result.getStatus());
            verify(vehicleService).delete(1L);
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException")
        void shouldPropagateNotFoundException() {
            when(vehicleService.delete(99L))
                    .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Vehicle not found")));

            assertThrows(ResourceNotFoundException.class,
                    () -> controller.delete(99L).await().indefinitely());
        }
    }
}