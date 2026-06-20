package br.com.fiap.postech.soat16.fase1.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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

import br.com.fiap.postech.soat16.fase1.dto.request.ServiceItemRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.ServiceItemResponseDto;
import br.com.fiap.postech.soat16.fase1.exception.ResourceNotFoundException;
import br.com.fiap.postech.soat16.fase1.service.ServiceItemService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceItemController — Unit Tests")
class ServiceItemControllerTest {

    @Mock
    private ServiceItemService service;

    private ServiceItemController controller;

    private static final UUID FIXED_UUID = UUID.fromString("d4a1f3a0-9b2e-4f3a-8c1d-1a2b3c4d5e6f");

    private ServiceItemResponseDto response;

    @BeforeEach
    void setUp() {
        controller = new ServiceItemController(service);
        response = new ServiceItemResponseDto(FIXED_UUID, "Troca de oleo", "Troca de oleo e filtro",
            new BigDecimal("80.00"), 30, true, OffsetDateTime.now());
    }

    private ServiceItemRequestDto request() {
        return new ServiceItemRequestDto("Troca de oleo", "Troca de oleo e filtro", new BigDecimal("80.00"), 30, true);
    }

    @Nested
    @DisplayName("GET /admin/services — listAll")
    class ListAll {

        @Test
        @DisplayName("should return list when services exist")
        void shouldReturnList() {
            when(service.listAll()).thenReturn(Uni.createFrom().item(List.of(response)));

            List<ServiceItemResponseDto> result = controller.listAll().await().indefinitely();

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
    @DisplayName("GET /admin/services/{id} — findById")
    class FindById {

        @Test
        @DisplayName("should return service when found")
        void shouldReturnWhenFound() {
            when(service.findById(FIXED_UUID)).thenReturn(Uni.createFrom().item(response));

            ServiceItemResponseDto result = controller.findById(FIXED_UUID).await().indefinitely();

            assertEquals("Troca de oleo", result.name());
            verify(service).findById(FIXED_UUID);
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException")
        void shouldPropagateNotFound() {
            when(service.findById(FIXED_UUID))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Service not found with id: " + FIXED_UUID)));

            assertThrows(ResourceNotFoundException.class,
                () -> controller.findById(FIXED_UUID).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("POST /admin/services — create")
    class Create {

        @Test
        @DisplayName("should return HTTP 201 with Location and body")
        void shouldReturn201() {
            ServiceItemRequestDto dto = request();
            when(service.create(dto)).thenReturn(Uni.createFrom().item(response));

            Response result = controller.create(dto).await().indefinitely();

            assertEquals(201, result.getStatus());
            assertEquals("/admin/services/" + FIXED_UUID, result.getLocation().toString());
            assertEquals(response, result.getEntity());
            verify(service).create(dto);
        }
    }

    @Nested
    @DisplayName("PUT /admin/services/{id} — update")
    class Update {

        @Test
        @DisplayName("should return updated body")
        void shouldReturnUpdated() {
            ServiceItemRequestDto dto = request();
            when(service.update(FIXED_UUID, dto)).thenReturn(Uni.createFrom().item(response));

            ServiceItemResponseDto result = controller.update(FIXED_UUID, dto).await().indefinitely();

            assertNotNull(result);
            verify(service).update(FIXED_UUID, dto);
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException")
        void shouldPropagateNotFound() {
            ServiceItemRequestDto dto = request();
            when(service.update(FIXED_UUID, dto))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Service not found with id: " + FIXED_UUID)));

            assertThrows(ResourceNotFoundException.class,
                () -> controller.update(FIXED_UUID, dto).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("DELETE /admin/services/{id} — delete")
    class Delete {

        @Test
        @DisplayName("should return HTTP 204 when delete succeeds")
        void shouldReturn204() {
            when(service.delete(FIXED_UUID)).thenReturn(Uni.createFrom().voidItem());

            Response result = controller.delete(FIXED_UUID).await().indefinitely();

            assertEquals(204, result.getStatus());
            verify(service).delete(FIXED_UUID);
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException")
        void shouldPropagateNotFound() {
            when(service.delete(FIXED_UUID))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Service not found with id: " + FIXED_UUID)));

            assertThrows(ResourceNotFoundException.class,
                () -> controller.delete(FIXED_UUID).await().indefinitely());
        }
    }
}
