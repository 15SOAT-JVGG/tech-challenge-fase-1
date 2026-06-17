package br.com.fiap.postech.soat16.fase1.controller;

import br.com.fiap.postech.soat16.fase1.dto.request.PartRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.PartResponseDto;
import br.com.fiap.postech.soat16.fase1.exception.BusinessException;
import br.com.fiap.postech.soat16.fase1.exception.ResourceNotFoundException;
import br.com.fiap.postech.soat16.fase1.model.PartType;
import br.com.fiap.postech.soat16.fase1.service.PartService;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartController — Unit Tests")
class PartControllerTest {

    @Mock
    private PartService service;

    private PartController controller;

    private static final Long ID = 1L;

    private PartResponseDto response;

    private static final UUID FIXED_UUID = UUID.fromString("c3b79cde-2872-4053-9622-37605bf124a3");

    @BeforeEach
    void setUp() {
        controller = new PartController(service);
        response = new PartResponseDto(FIXED_UUID, "Óleo 5W30", "Óleo sintético", new BigDecimal("49.90"),
            10, "L", 5, PartType.INSUMO, false, LocalDateTime.now());
    }

    private PartRequestDto request() {
        return new PartRequestDto("Óleo 5W30", "Óleo sintético", new BigDecimal("49.90"), 10, "L", 5, PartType.INSUMO);
    }

    @Nested
    @DisplayName("GET /admin/parts — listAll")
    class ListAll {

        @Test
        @DisplayName("should return list when parts exist")
        void shouldReturnList() {
            when(service.listAll()).thenReturn(Uni.createFrom().item(List.of(response)));

            List<PartResponseDto> result = controller.listAll().await().indefinitely();

            assertEquals(1, result.size());
            verify(service).listAll();
        }

        @Test
        @DisplayName("should propagate exception from service")
        void shouldPropagate() {
            when(service.listAll()).thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

            assertThrows(RuntimeException.class, () -> controller.listAll().await().indefinitely());
        }
    }

    @Nested
    @DisplayName("GET /admin/parts/{id} — findById")
    class FindById {

        @Test
        @DisplayName("should return part when found")
        void shouldReturnWhenFound() {
            when(service.findById(ID)).thenReturn(Uni.createFrom().item(response));

            PartResponseDto result = controller.findById(ID).await().indefinitely();

            assertEquals("Óleo 5W30", result.name());
            verify(service).findById(ID);
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException")
        void shouldPropagateNotFound() {
            when(service.findById(ID))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException(PartService.PECA_INSUMO, ID)));

            assertThrows(ResourceNotFoundException.class,
                () -> controller.findById(ID).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("GET /admin/parts/low-stock — findLowStock")
    class FindLowStock {

        @Test
        @DisplayName("should return low-stock parts")
        void shouldReturnLowStock() {
            when(service.findLowStock()).thenReturn(Uni.createFrom().item(List.of(response)));

            List<PartResponseDto> result = controller.findLowStock().await().indefinitely();

            assertEquals(1, result.size());
            verify(service).findLowStock();
        }
    }

    @Nested
    @DisplayName("POST /admin/parts — create")
    class Create {

        @Test
        @DisplayName("should return HTTP 201 with Location and body")
        void shouldReturn201() {
            PartRequestDto dto = request();
            when(service.create(dto)).thenReturn(Uni.createFrom().item(response));

            Response result = controller.create(dto).await().indefinitely();

            assertEquals(201, result.getStatus());
            assertEquals("/admin/parts/c3b79cde-2872-4053-9622-37605bf124a3", result.getLocation().toString());
            assertEquals(response, result.getEntity());
            verify(service).create(dto);
        }
    }

    @Nested
    @DisplayName("PUT /admin/parts/{id} — update")
    class Update {

        @Test
        @DisplayName("should return updated body")
        void shouldReturnUpdated() {
            PartRequestDto dto = request();
            when(service.update(ID, dto)).thenReturn(Uni.createFrom().item(response));

            PartResponseDto result = controller.update(ID, dto).await().indefinitely();

            assertNotNull(result);
            verify(service).update(ID, dto);
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException")
        void shouldPropagateNotFound() {
            PartRequestDto dto = request();
            when(service.update(ID, dto))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException(PartService.PECA_INSUMO, ID)));

            assertThrows(ResourceNotFoundException.class,
                () -> controller.update(ID, dto).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("PATCH /admin/parts/{id}/stock — adjustStock")
    class AdjustStock {

        @Test
        @DisplayName("should return adjusted body")
        void shouldReturnAdjusted() {
            when(service.adjustStock(ID, 5)).thenReturn(Uni.createFrom().item(response));

            PartResponseDto result = controller.adjustStock(ID, 5).await().indefinitely();

            assertNotNull(result);
            verify(service).adjustStock(ID, 5);
        }

        @Test
        @DisplayName("should propagate BusinessException on insufficient stock")
        void shouldPropagateBusiness() {
            when(service.adjustStock(ID, -50))
                .thenReturn(Uni.createFrom().failure(new BusinessException("Estoque insuficiente")));

            assertThrows(BusinessException.class,
                () -> controller.adjustStock(ID, -50).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("DELETE /admin/parts/{id} — delete")
    class Delete {

        @Test
        @DisplayName("should return HTTP 204 when delete succeeds")
        void shouldReturn204() {
            when(service.delete(ID)).thenReturn(Uni.createFrom().voidItem());

            Response result = controller.delete(ID).await().indefinitely();

            assertEquals(204, result.getStatus());
            verify(service).delete(ID);
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException")
        void shouldPropagateNotFound() {
            when(service.delete(ID))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException(PartService.PECA_INSUMO, ID)));

            assertThrows(ResourceNotFoundException.class,
                () -> controller.delete(ID).await().indefinitely());
        }
    }
}
