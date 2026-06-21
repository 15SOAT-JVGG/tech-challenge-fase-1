package br.com.fiap.postech.soat16.fase1.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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
import br.com.fiap.postech.soat16.fase1.model.ServiceItem;
import br.com.fiap.postech.soat16.fase1.repository.ServiceItemRepository;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceItemService — Unit Tests")
class ServiceItemServiceTest {

    @Mock
    private ServiceItemRepository repository;

    private ServiceItemService service;

    private static final UUID ID = UUID.randomUUID();

    private ServiceItem entity;

    @BeforeEach
    void setUp() {
        service = new ServiceItemService(repository);
        entity = new ServiceItem();
        entity.setId(ID);
        entity.setName("Troca de óleo");
        entity.setDescription("Troca de óleo e filtro");
        entity.setBasePrice(new BigDecimal("120.00"));
        entity.setEstimatedDurationMinutes(30);
        entity.setActive(true);
    }

    private ServiceItemRequestDto request(String name, Boolean active) {
        return new ServiceItemRequestDto(name, "desc", new BigDecimal("120.00"), 30, active);
    }

    @Nested
    @DisplayName("listAll")
    class ListAll {

        @Test
        @DisplayName("should map all services to response")
        void shouldMapAll() {
            when(repository.listAll()).thenReturn(Uni.createFrom().item(List.of(entity)));

            List<ServiceItemResponseDto> result = service.listAll().await().indefinitely();

            assertEquals(1, result.size());
            assertEquals("Troca de óleo", result.getFirst().name());
        }

        @Test
        @DisplayName("should return empty list when no services exist")
        void shouldReturnEmpty() {
            when(repository.listAll()).thenReturn(Uni.createFrom().item(List.of()));

            assertTrue(service.listAll().await().indefinitely().isEmpty());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return service when found")
        void shouldReturnWhenFound() {
            when(repository.findById(ID)).thenReturn(Uni.createFrom().item(entity));

            ServiceItemResponseDto result = service.findById(ID).await().indefinitely();

            assertNotNull(result);
            assertEquals("Troca de óleo", result.name());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when missing")
        void shouldThrowWhenMissing() {
            when(repository.findById(ID)).thenReturn(Uni.createFrom().nullItem());

            assertThrows(ResourceNotFoundException.class,
                () -> service.findById(ID).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should persist service and default active to true when omitted")
        void shouldPersistDefaultingActive() {
            when(repository.persist(any(ServiceItem.class))).thenReturn(Uni.createFrom().item(entity));

            ServiceItemResponseDto result = service.create(request("Alinhamento", null)).await().indefinitely();

            assertNotNull(result);
            verify(repository).persist(any(ServiceItem.class));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update entity and return response")
        void shouldUpdate() {
            when(repository.findById(ID)).thenReturn(Uni.createFrom().item(entity));
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));

            ServiceItemResponseDto result = service.update(ID, request("Balanceamento", false))
                .await().indefinitely();

            assertNotNull(result);
            assertEquals("Balanceamento", entity.getName());
            assertFalse(entity.isActive());
            verify(repository).persist(entity);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when missing")
        void shouldThrowWhenMissing() {
            when(repository.findById(ID)).thenReturn(Uni.createFrom().nullItem());

            assertThrows(ResourceNotFoundException.class,
                () -> service.update(ID, request("X", true)).await().indefinitely());

            verify(repository, never()).persist(any(ServiceItem.class));
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete when record exists")
        void shouldDelete() {
            when(repository.deleteById(ID)).thenReturn(Uni.createFrom().item(true));

            assertDoesNotThrow(() -> service.delete(ID).await().indefinitely());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when nothing was deleted")
        void shouldThrowWhenNothingDeleted() {
            when(repository.deleteById(ID)).thenReturn(Uni.createFrom().item(false));

            assertThrows(ResourceNotFoundException.class,
                () -> service.delete(ID).await().indefinitely());
        }
    }
}
