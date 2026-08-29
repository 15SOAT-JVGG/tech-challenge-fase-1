package br.com.fiap.postech.soat16.fase1.part.adapter.in.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.postech.soat16.fase1.part.adapter.in.rest.dto.request.PartRequestDto;
import br.com.fiap.postech.soat16.fase1.part.adapter.in.rest.dto.response.PartResponseDto;
import br.com.fiap.postech.soat16.fase1.part.adapter.in.rest.mapper.PartRestMapper;
import br.com.fiap.postech.soat16.fase1.part.application.PartService;
import br.com.fiap.postech.soat16.fase1.part.application.result.PartResult;
import br.com.fiap.postech.soat16.fase1.part.domain.model.enums.PartType;
import br.com.fiap.postech.soat16.fase1.shared.domain.exception.BusinessException;
import br.com.fiap.postech.soat16.fase1.shared.domain.exception.ResourceNotFoundException;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartController — Unit Tests")
class PartControllerTest {

    private static final UUID ID =
            UUID.fromString("c3b79cde-2872-4053-9622-37605bf124a3");

    @Mock
    private PartService service;

    private PartController controller;
    private PartResult result;

    @BeforeEach
    void setUp() {
        controller = new PartController(service);
        result = new PartResult(
                ID,
                "Óleo 5W30",
                "Óleo sintético",
                new BigDecimal("49.90"),
                10,
                "L",
                5,
                PartType.SUPPLY,
                false,
                LocalDateTime.now());
    }

    private PartRequestDto request() {
        return new PartRequestDto(
                "Óleo 5W30",
                "Óleo sintético",
                new BigDecimal("49.90"),
                10,
                "L",
                5,
                PartType.SUPPLY);
    }

    @Nested
    @DisplayName("GET /admin/parts")
    class ListAll {

        @Test
        @DisplayName("should return list when parts exist")
        void shouldReturnList() {
            when(service.listAll()).thenReturn(Uni.createFrom().item(List.of(result)));

            List<PartResponseDto> response = controller.listAll().await().indefinitely();

            assertEquals(1, response.size());
            verify(service).listAll();
        }

        @Test
        @DisplayName("should propagate exception from service")
        void shouldPropagate() {
            when(service.listAll())
                    .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

            assertThrows(
                    RuntimeException.class,
                    () -> controller.listAll().await().indefinitely());
        }
    }

    @Nested
    @DisplayName("GET /admin/parts/{id}")
    class FindById {

        @Test
        @DisplayName("should return part when found")
        void shouldReturnWhenFound() {
            when(service.findById(PartRestMapper.toQuery(ID)))
                    .thenReturn(Uni.createFrom().item(result));

            PartResponseDto response = controller.findById(ID).await().indefinitely();

            assertEquals("Óleo 5W30", response.name());
            verify(service).findById(PartRestMapper.toQuery(ID));
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException")
        void shouldPropagateNotFound() {
            when(service.findById(PartRestMapper.toQuery(ID)))
                    .thenReturn(Uni.createFrom().failure(
                            new ResourceNotFoundException(PartService.PECA_INSUMO, ID)));

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> controller.findById(ID).await().indefinitely());
        }
    }

    @Test
    @DisplayName("GET /admin/parts/low-stock should return low-stock parts")
    void shouldReturnLowStock() {
        when(service.findLowStock()).thenReturn(Uni.createFrom().item(List.of(result)));

        List<PartResponseDto> response = controller.findLowStock().await().indefinitely();

        assertEquals(1, response.size());
        verify(service).findLowStock();
    }

    @Test
    @DisplayName("POST /admin/parts should return HTTP 201 with Location and body")
    void createShouldReturn201() {
        PartRequestDto request = request();
        when(service.create(PartRestMapper.toCreateCommand(request)))
                .thenReturn(Uni.createFrom().item(result));

        Response response = controller.create(request).await().indefinitely();

        assertEquals(201, response.getStatus());
        assertEquals("/admin/parts/" + ID, response.getLocation().toString());
        assertEquals(PartRestMapper.toResponse(result), response.getEntity());
        verify(service).create(PartRestMapper.toCreateCommand(request));
    }

    @Nested
    @DisplayName("PUT /admin/parts/{id}")
    class Update {

        @Test
        @DisplayName("should return updated body")
        void shouldReturnUpdated() {
            PartRequestDto request = request();
            when(service.update(PartRestMapper.toUpdateCommand(ID, request)))
                    .thenReturn(Uni.createFrom().item(result));

            PartResponseDto response = controller.update(ID, request).await().indefinitely();

            assertNotNull(response);
            verify(service).update(PartRestMapper.toUpdateCommand(ID, request));
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException")
        void shouldPropagateNotFound() {
            PartRequestDto request = request();
            when(service.update(PartRestMapper.toUpdateCommand(ID, request)))
                    .thenReturn(Uni.createFrom().failure(
                            new ResourceNotFoundException(PartService.PECA_INSUMO, ID)));

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> controller.update(ID, request).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("PATCH /admin/parts/{id}/stock")
    class AdjustStock {

        @Test
        @DisplayName("should return adjusted body")
        void shouldReturnAdjusted() {
            when(service.adjustStock(PartRestMapper.toStockCommand(ID, 5)))
                    .thenReturn(Uni.createFrom().item(result));

            PartResponseDto response = controller.adjustStock(ID, 5).await().indefinitely();

            assertNotNull(response);
            verify(service).adjustStock(PartRestMapper.toStockCommand(ID, 5));
        }

        @Test
        @DisplayName("should propagate BusinessException on insufficient stock")
        void shouldPropagateBusiness() {
            when(service.adjustStock(PartRestMapper.toStockCommand(ID, -50)))
                    .thenReturn(Uni.createFrom().failure(
                            new BusinessException("Insufficient stock")));

            assertThrows(
                    BusinessException.class,
                    () -> controller.adjustStock(ID, -50).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("DELETE /admin/parts/{id}")
    class Delete {

        @Test
        @DisplayName("should return HTTP 204 when delete succeeds")
        void shouldReturn204() {
            when(service.delete(PartRestMapper.toDeleteCommand(ID)))
                    .thenReturn(Uni.createFrom().voidItem());

            Response response = controller.delete(ID).await().indefinitely();

            assertEquals(204, response.getStatus());
            verify(service).delete(PartRestMapper.toDeleteCommand(ID));
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException")
        void shouldPropagateNotFound() {
            when(service.delete(PartRestMapper.toDeleteCommand(ID)))
                    .thenReturn(Uni.createFrom().failure(
                            new ResourceNotFoundException(PartService.PECA_INSUMO, ID)));

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> controller.delete(ID).await().indefinitely());
        }
    }
}
