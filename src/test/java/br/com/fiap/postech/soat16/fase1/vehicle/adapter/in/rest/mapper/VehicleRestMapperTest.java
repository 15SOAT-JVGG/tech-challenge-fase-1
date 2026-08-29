package br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.shared.application.result.PagedResult;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.dto.request.VehicleFilterDto;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.dto.request.VehicleRequestDto;
import br.com.fiap.postech.soat16.fase1.vehicle.application.query.VehicleQuery;
import br.com.fiap.postech.soat16.fase1.vehicle.application.result.VehicleResult;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.enums.VehicleType;

@DisplayName("Mapeamento REST de veículos — testes unitários")
class VehicleRestMapperTest {

    private static final UUID VEHICLE_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final OffsetDateTime CREATED_AT =
            OffsetDateTime.parse("2026-08-29T15:00:00-03:00");

    @Test
    @DisplayName("mapeia paginação, filtros e ordenação para VehicleQuery")
    void mapsQuery() {
        PageableRequestDto pageable = mock(PageableRequestDto.class);
        VehicleFilterDto filter = mock(VehicleFilterDto.class);
        when(pageable.getPage()).thenReturn(2);
        when(pageable.getSize()).thenReturn(20);
        when(pageable.getSortParameters())
                .thenReturn(List.of("manufacturer,asc", "model,desc"));
        when(filter.getLicensePlate()).thenReturn("ABC");
        when(filter.getManufacturer()).thenReturn("Toyota");
        when(filter.getModel()).thenReturn("Corolla");

        var query = VehicleRestMapper.toQuery(pageable, filter);

        assertEquals(2, query.page());
        assertEquals(20, query.size());
        assertEquals("ABC", query.licensePlate());
        assertEquals("Toyota", query.manufacturer());
        assertEquals("Corolla", query.model());
        assertEquals(2, query.orders().size());
        assertEquals("manufacturer", query.orders().getFirst().field());
        assertEquals(
                VehicleQuery.Direction.ASCENDING,
                query.orders().getFirst().direction());
        assertEquals("model", query.orders().getLast().field());
        assertEquals(
                VehicleQuery.Direction.DESCENDING,
                query.orders().getLast().direction());
    }

    @Test
    @DisplayName("mapeia todos os campos para o comando de criação")
    void mapsCreateCommand() {
        var create = VehicleRestMapper.toCreateCommand(request());

        assertEquals("ABC1234", create.licensePlate());
        assertEquals(CUSTOMER_ID, create.customerId());
        assertEquals("Toyota", create.manufacturer());
        assertEquals("Corolla", create.model());
        assertEquals("Prata", create.color());
        assertEquals(2020, create.year());
        assertEquals(50_000L, create.kmDriven());
        assertEquals(VehicleType.CAR, create.type());
    }

    @Test
    @DisplayName("mapeia todos os campos para o comando de atualização")
    void mapsUpdateCommand() {
        var update = VehicleRestMapper.toUpdateCommand(VEHICLE_ID, request());

        assertEquals(VEHICLE_ID, update.id());
        assertEquals("ABC1234", update.licensePlate());
        assertEquals("Toyota", update.manufacturer());
        assertEquals("Corolla", update.model());
        assertEquals("Prata", update.color());
        assertEquals(2020, update.year());
        assertEquals(50_000L, update.kmDriven());
        assertEquals(VehicleType.CAR, update.type());
    }

    @Test
    @DisplayName("mapeia todos os campos do resultado para a resposta REST")
    void mapsResponse() {
        var response = VehicleRestMapper.toResponse(result(CREATED_AT));

        assertEquals(VEHICLE_ID, response.id());
        assertEquals("ABC1234", response.licensePlate());
        assertEquals("Toyota", response.manufacturer());
        assertEquals("Corolla", response.model());
        assertEquals("Prata", response.color());
        assertEquals(2020, response.year());
        assertEquals(50_000L, response.kmDriven());
        assertEquals(VehicleType.CAR, response.type());
        assertEquals(CUSTOMER_ID, response.customerId());
        assertEquals(CREATED_AT, response.createdAt());
    }

    @Test
    @DisplayName("preserva conteúdo e metadados ao mapear uma página")
    void mapsPagedResponse() {
        VehicleResult result = result(CREATED_AT);

        var page = VehicleRestMapper.toResponse(
                PagedResult.of(List.of(result), 1, 5, 12));

        assertEquals(1, page.content().size());
        assertEquals(VEHICLE_ID, page.content().getFirst().id());
        assertEquals(1, page.pagination().page());
        assertEquals(5, page.pagination().size());
        assertEquals(12L, page.pagination().totalElements());
        assertEquals(3, page.pagination().totalPages());
        assertTrue(page.pagination().hasPrevious());
        assertTrue(page.pagination().hasNext());
    }

    @Test
    @DisplayName("mapeia corretamente uma página vazia")
    void mapsEmptyPage() {
        var page = VehicleRestMapper.toResponse(
                PagedResult.of(List.of(), 0, 10, 0));

        assertTrue(page.content().isEmpty());
        assertEquals(0L, page.pagination().totalElements());
        assertFalse(page.pagination().hasPrevious());
        assertFalse(page.pagination().hasNext());
    }

    private VehicleRequestDto request() {
        return new VehicleRequestDto(
                CUSTOMER_ID,
                "abc-1234",
                "Toyota",
                "Corolla",
                "Prata",
                2020,
                50_000L,
                VehicleType.CAR);
    }

    private VehicleResult result(OffsetDateTime createdAt) {
        return new VehicleResult(
                VEHICLE_ID,
                "ABC1234",
                "Toyota",
                "Corolla",
                "Prata",
                2020,
                50_000L,
                VehicleType.CAR,
                CUSTOMER_ID,
                createdAt);
    }
}
