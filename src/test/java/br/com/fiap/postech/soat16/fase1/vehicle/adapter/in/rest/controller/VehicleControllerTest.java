package br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.shared.application.result.PagedResult;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.dto.request.VehicleFilterDto;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.dto.request.VehicleRequestDto;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.dto.response.VehicleResponseDto;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.mapper.VehicleRestMapper;
import br.com.fiap.postech.soat16.fase1.vehicle.application.VehicleService;
import br.com.fiap.postech.soat16.fase1.vehicle.application.result.VehicleResult;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.exception.DuplicateLicensePlateException;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.exception.VehicleNotFoundException;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.enums.VehicleType;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("Controlador de veículos — testes unitários")
class VehicleControllerTest {

    private static final UUID VEHICLE_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @Mock
    VehicleService service;

    private VehicleController controller;
    private VehicleResult vehicle;
    private VehicleRequestDto request;

    @BeforeEach
    void setUp() {
        controller = new VehicleController(service);
        vehicle = new VehicleResult(
                VEHICLE_ID,
                "ABC1234",
                "Toyota",
                "Corolla",
                "Prata",
                2020,
                50_000L,
                VehicleType.CAR,
                CUSTOMER_ID,
                null);
        request = new VehicleRequestDto(
                CUSTOMER_ID,
                "ABC1234",
                "Toyota",
                "Corolla",
                "Prata",
                2020,
                50_000L,
                VehicleType.CAR);
    }

    @Nested
    @DisplayName("GET /v1/vehicle")
    class ListAll {

        @Test
        @DisplayName("retorna a lista paginada mapeada")
        void returnsPagedVehicles() {
            PageableRequestDto pageable = pageable(0, 10);
            VehicleFilterDto filter = mock(VehicleFilterDto.class);
            var query = VehicleRestMapper.toQuery(pageable, filter);
            when(service.listAll(query))
                    .thenReturn(Uni.createFrom().item(
                            PagedResult.of(List.of(vehicle), 0, 10, 1)));

            PageableResponseDto<VehicleResponseDto> result =
                    controller.listAll(pageable, filter).await().indefinitely();

            assertEquals(1, result.content().size());
            assertEquals("ABC1234", result.content().getFirst().licensePlate());
            assertEquals(1L, result.pagination().totalElements());
            verify(service).listAll(query);
        }

        @Test
        @DisplayName("retorna página vazia quando não há veículos")
        void returnsEmptyPage() {
            PageableRequestDto pageable = pageable(0, 10);
            VehicleFilterDto filter = mock(VehicleFilterDto.class);
            var query = VehicleRestMapper.toQuery(pageable, filter);
            when(service.listAll(query))
                    .thenReturn(Uni.createFrom().item(PagedResult.of(List.of(), 0, 10, 0)));

            PageableResponseDto<VehicleResponseDto> result =
                    controller.listAll(pageable, filter).await().indefinitely();

            assertTrue(result.content().isEmpty());
            assertEquals(0L, result.pagination().totalElements());
        }

        @Test
        @DisplayName("propaga falhas da aplicação")
        void propagatesFailure() {
            PageableRequestDto pageable = pageable(0, 10);
            VehicleFilterDto filter = mock(VehicleFilterDto.class);
            var query = VehicleRestMapper.toQuery(pageable, filter);
            when(service.listAll(query))
                    .thenReturn(Uni.createFrom().failure(new IllegalStateException("falha")));

            assertThrows(
                    IllegalStateException.class,
                    () -> controller.listAll(pageable, filter).await().indefinitely());
        }

        private PageableRequestDto pageable(int page, int size) {
            PageableRequestDto pageable = mock(PageableRequestDto.class);
            when(pageable.getPage()).thenReturn(page);
            when(pageable.getSize()).thenReturn(size);
            when(pageable.getSortParameters()).thenReturn(List.of());
            return pageable;
        }
    }

    @Nested
    @DisplayName("GET /v1/vehicle/{id}")
    class FindById {

        @Test
        @DisplayName("retorna o veículo encontrado")
        void returnsVehicle() {
            when(service.findById(VEHICLE_ID))
                    .thenReturn(Uni.createFrom().item(vehicle));

            VehicleResponseDto result =
                    controller.findById(VEHICLE_ID).await().indefinitely();

            assertEquals(VEHICLE_ID, result.id());
            assertEquals("ABC1234", result.licensePlate());
            verify(service).findById(VEHICLE_ID);
        }

        @Test
        @DisplayName("propaga a ausência do veículo")
        void propagatesMissingVehicle() {
            when(service.findById(VEHICLE_ID))
                    .thenReturn(Uni.createFrom().failure(new VehicleNotFoundException()));

            assertThrows(
                    VehicleNotFoundException.class,
                    () -> controller.findById(VEHICLE_ID).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("GET /v1/vehicle/by-license-plate/{license_plate}")
    class FindByLicensePlate {

        @Test
        @DisplayName("expõe a rota de consulta por placa")
        void exposesLicensePlateRoute() throws NoSuchMethodException {
            Path path = VehicleController.class
                    .getDeclaredMethod("findByLicensePlate", String.class)
                    .getAnnotation(Path.class);

            assertEquals("/by-license-plate/{license_plate}", path.value());
        }

        @Test
        @DisplayName("retorna o veículo encontrado pela placa")
        void returnsVehicle() {
            when(service.findByLicensePlate("abc-1234"))
                    .thenReturn(Uni.createFrom().item(vehicle));

            VehicleResponseDto result =
                    controller.findByLicensePlate("abc-1234").await().indefinitely();

            assertEquals("ABC1234", result.licensePlate());
            assertEquals("Toyota", result.manufacturer());
            verify(service).findByLicensePlate("abc-1234");
        }

        @Test
        @DisplayName("propaga a ausência da placa")
        void propagatesMissingPlate() {
            when(service.findByLicensePlate("XYZ9999"))
                    .thenReturn(Uni.createFrom().failure(new VehicleNotFoundException("XYZ9999")));

            assertThrows(
                    VehicleNotFoundException.class,
                    () -> controller.findByLicensePlate("XYZ9999").await().indefinitely());
        }
    }

    @Nested
    @DisplayName("POST /v1/vehicle")
    class Create {

        @Test
        @DisplayName("retorna HTTP 201 quando a criação é concluída")
        void returnsCreated() {
            when(service.create(VehicleRestMapper.toCreateCommand(request)))
                    .thenReturn(Uni.createFrom().voidItem());

            Response result = controller.create(request).await().indefinitely();

            assertEquals(Response.Status.CREATED.getStatusCode(), result.getStatus());
            verify(service).create(VehicleRestMapper.toCreateCommand(request));
        }

        @Test
        @DisplayName("propaga a rejeição de placa duplicada")
        void propagatesDuplicatePlate() {
            when(service.create(VehicleRestMapper.toCreateCommand(request)))
                    .thenReturn(Uni.createFrom().failure(
                            new DuplicateLicensePlateException()));

            assertThrows(
                    DuplicateLicensePlateException.class,
                    () -> controller.create(request).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("PUT /v1/vehicle/{id}")
    class Update {

        @Test
        @DisplayName("retorna HTTP 200 com o veículo atualizado")
        void returnsUpdatedVehicle() {
            when(service.update(VehicleRestMapper.toUpdateCommand(VEHICLE_ID, request)))
                    .thenReturn(Uni.createFrom().item(vehicle));

            Response result = controller.update(VEHICLE_ID, request).await().indefinitely();

            assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
            assertEquals(VEHICLE_ID, ((VehicleResponseDto) result.getEntity()).id());
            verify(service).update(VehicleRestMapper.toUpdateCommand(VEHICLE_ID, request));
        }

        @Test
        @DisplayName("propaga a ausência do veículo")
        void propagatesMissingVehicle() {
            when(service.update(VehicleRestMapper.toUpdateCommand(VEHICLE_ID, request)))
                    .thenReturn(Uni.createFrom().failure(new VehicleNotFoundException()));

            assertThrows(
                    VehicleNotFoundException.class,
                    () -> controller.update(VEHICLE_ID, request).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("DELETE /v1/vehicle/{id}")
    class Delete {

        @Test
        @DisplayName("retorna HTTP 204 quando a remoção é concluída")
        void returnsNoContent() {
            when(service.delete(VEHICLE_ID)).thenReturn(Uni.createFrom().voidItem());

            Response result = controller.delete(VEHICLE_ID).await().indefinitely();

            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), result.getStatus());
            verify(service).delete(VEHICLE_ID);
        }

        @Test
        @DisplayName("propaga a ausência do veículo")
        void propagatesMissingVehicle() {
            when(service.delete(VEHICLE_ID))
                    .thenReturn(Uni.createFrom().failure(new VehicleNotFoundException()));

            assertThrows(
                    VehicleNotFoundException.class,
                    () -> controller.delete(VEHICLE_ID).await().indefinitely());
        }
    }

}
