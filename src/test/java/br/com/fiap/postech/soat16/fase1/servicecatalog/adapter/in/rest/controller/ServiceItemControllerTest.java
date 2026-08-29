package br.com.fiap.postech.soat16.fase1.servicecatalog.adapter.in.rest.controller;

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

import br.com.fiap.postech.soat16.fase1.servicecatalog.adapter.in.rest.dto.request.ServiceItemRequestDto;
import br.com.fiap.postech.soat16.fase1.servicecatalog.adapter.in.rest.dto.response.ServiceItemResponseDto;
import br.com.fiap.postech.soat16.fase1.servicecatalog.adapter.in.rest.mapper.ServiceCatalogRestMapper;
import br.com.fiap.postech.soat16.fase1.servicecatalog.application.ServiceItemService;
import br.com.fiap.postech.soat16.fase1.servicecatalog.application.result.ServiceItemResult;
import br.com.fiap.postech.soat16.fase1.shared.domain.exception.ResourceNotFoundException;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceItemController — Unit Tests")
class ServiceItemControllerTest {

    private static final UUID ID =
            UUID.fromString("d4a1f3a0-9b2e-4f3a-8c1d-1a2b3c4d5e6f");

    @Mock
    private ServiceItemService service;

    private ServiceItemController controller;
    private ServiceItemResult result;

    @BeforeEach
    void setUp() {
        controller = new ServiceItemController(service);
        result = new ServiceItemResult(
                ID,
                "Troca de oleo",
                "Troca de oleo e filtro",
                new BigDecimal("80.00"),
                30,
                true,
                OffsetDateTime.now());
    }

    private ServiceItemRequestDto request() {
        return new ServiceItemRequestDto(
                "Troca de oleo",
                "Troca de oleo e filtro",
                new BigDecimal("80.00"),
                30,
                true);
    }

    @Nested
    @DisplayName("GET /admin/services")
    class ListAll {

        @Test
        @DisplayName("should return list when services exist")
        void shouldReturnList() {
            when(service.listAll()).thenReturn(Uni.createFrom().item(List.of(result)));

            List<ServiceItemResponseDto> response =
                    controller.listAll().await().indefinitely();

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
    @DisplayName("GET /admin/services/{id}")
    class FindById {

        @Test
        @DisplayName("should return service when found")
        void shouldReturnWhenFound() {
            when(service.findById(ServiceCatalogRestMapper.toQuery(ID)))
                    .thenReturn(Uni.createFrom().item(result));

            ServiceItemResponseDto response =
                    controller.findById(ID).await().indefinitely();

            assertEquals("Troca de oleo", response.name());
            verify(service).findById(ServiceCatalogRestMapper.toQuery(ID));
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException")
        void shouldPropagateNotFound() {
            when(service.findById(ServiceCatalogRestMapper.toQuery(ID)))
                    .thenReturn(Uni.createFrom().failure(
                            new ResourceNotFoundException("Service not found with id: " + ID)));

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> controller.findById(ID).await().indefinitely());
        }
    }

    @Test
    @DisplayName("POST /admin/services should return HTTP 201 with Location and body")
    void createShouldReturn201() {
        ServiceItemRequestDto request = request();
        when(service.create(ServiceCatalogRestMapper.toCreateCommand(request)))
                .thenReturn(Uni.createFrom().item(result));

        Response response = controller.create(request).await().indefinitely();

        assertEquals(201, response.getStatus());
        assertEquals("/admin/services/" + ID, response.getLocation().toString());
        assertEquals(ServiceCatalogRestMapper.toResponse(result), response.getEntity());
        verify(service).create(ServiceCatalogRestMapper.toCreateCommand(request));
    }

    @Nested
    @DisplayName("PUT /admin/services/{id}")
    class Update {

        @Test
        @DisplayName("should return updated body")
        void shouldReturnUpdated() {
            ServiceItemRequestDto request = request();
            when(service.update(ServiceCatalogRestMapper.toUpdateCommand(ID, request)))
                    .thenReturn(Uni.createFrom().item(result));

            ServiceItemResponseDto response =
                    controller.update(ID, request).await().indefinitely();

            assertNotNull(response);
            verify(service).update(ServiceCatalogRestMapper.toUpdateCommand(ID, request));
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException")
        void shouldPropagateNotFound() {
            ServiceItemRequestDto request = request();
            when(service.update(ServiceCatalogRestMapper.toUpdateCommand(ID, request)))
                    .thenReturn(Uni.createFrom().failure(
                            new ResourceNotFoundException("Service not found with id: " + ID)));

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> controller.update(ID, request).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("DELETE /admin/services/{id}")
    class Delete {

        @Test
        @DisplayName("should return HTTP 204 when delete succeeds")
        void shouldReturn204() {
            when(service.delete(ServiceCatalogRestMapper.toDeleteCommand(ID)))
                    .thenReturn(Uni.createFrom().voidItem());

            Response response = controller.delete(ID).await().indefinitely();

            assertEquals(204, response.getStatus());
            verify(service).delete(ServiceCatalogRestMapper.toDeleteCommand(ID));
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException")
        void shouldPropagateNotFound() {
            when(service.delete(ServiceCatalogRestMapper.toDeleteCommand(ID)))
                    .thenReturn(Uni.createFrom().failure(
                            new ResourceNotFoundException("Service not found with id: " + ID)));

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> controller.delete(ID).await().indefinitely());
        }
    }
}
