package br.com.fiap.postech.soat16.fase1.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

import br.com.fiap.postech.soat16.fase1.dto.request.PartRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.PartResponseDto;
import br.com.fiap.postech.soat16.fase1.exception.BusinessException;
import br.com.fiap.postech.soat16.fase1.exception.ResourceNotFoundException;
import br.com.fiap.postech.soat16.fase1.model.Part;
import br.com.fiap.postech.soat16.fase1.model.PartType;
import br.com.fiap.postech.soat16.fase1.repository.PartRepository;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartService — Unit Tests")
class PartServiceTest {

    @Mock
    private PartRepository repository;

    private PartService service;

    private static final UUID ID = UUID.fromString("c3b79cde-2872-4053-9622-37605bf124a3");

    private Part entity;

    @BeforeEach
    void setUp() {
        service = new PartService(repository);
        entity = new Part("Óleo 5W30", "Óleo sintético", new BigDecimal("49.90"), 10, "L", 5, PartType.INSUMO);
    }

    private PartRequestDto request(int stockQuantity) {
        return new PartRequestDto("Óleo 5W30", "Óleo sintético", new BigDecimal("49.90"),
            stockQuantity, "L", 5, PartType.INSUMO);
    }

    @Nested
    @DisplayName("listAll")
    class ListAll {

        @Test
        @DisplayName("should map all parts to response")
        void shouldMapAll() {
            when(repository.listAll()).thenReturn(Uni.createFrom().item(List.of(entity)));

            List<PartResponseDto> result = service.listAll().await().indefinitely();

            assertEquals(1, result.size());
            assertEquals("Óleo 5W30", result.getFirst().name());
        }

        @Test
        @DisplayName("should return empty list when no parts exist")
        void shouldReturnEmpty() {
            when(repository.listAll()).thenReturn(Uni.createFrom().item(List.of()));

            assertTrue(service.listAll().await().indefinitely().isEmpty());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return part when found")
        void shouldReturnWhenFound() {
            when(repository.findById(ID)).thenReturn(Uni.createFrom().item(entity));

            PartResponseDto result = service.findById(ID).await().indefinitely();

            assertNotNull(result);
            assertEquals("Óleo 5W30", result.name());
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
    @DisplayName("findLowStock")
    class FindLowStock {

        @Test
        @DisplayName("should map low-stock parts to response")
        void shouldMapLowStock() {
            when(repository.findLowStock()).thenReturn(Uni.createFrom().item(List.of(entity)));

            List<PartResponseDto> result = service.findLowStock().await().indefinitely();

            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should persist part and return response")
        void shouldPersist() {
            when(repository.persist(any(Part.class))).thenReturn(Uni.createFrom().item(entity));

            PartResponseDto result = service.create(request(10)).await().indefinitely();

            assertNotNull(result);
            verify(repository).persist(any(Part.class));
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

            PartResponseDto result = service.update(ID, request(20)).await().indefinitely();

            assertNotNull(result);
            assertEquals(20, entity.getStockQuantity());
            verify(repository).persist(entity);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when missing")
        void shouldThrowWhenMissing() {
            when(repository.findById(ID)).thenReturn(Uni.createFrom().nullItem());

            assertThrows(ResourceNotFoundException.class,
                () -> service.update(ID, request(20)).await().indefinitely());

            verify(repository, never()).persist(any(Part.class));
        }
    }

    @Nested
    @DisplayName("adjustStock")
    class AdjustStock {

        @Test
        @DisplayName("should increase stock on positive adjustment")
        void shouldIncrease() {
            when(repository.findById(ID)).thenReturn(Uni.createFrom().item(entity));
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));

            PartResponseDto result = service.adjustStock(ID, 5).await().indefinitely();

            assertEquals(15, result.stockQuantity());
        }

        @Test
        @DisplayName("should decrease stock on negative adjustment")
        void shouldDecrease() {
            when(repository.findById(ID)).thenReturn(Uni.createFrom().item(entity));
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));

            PartResponseDto result = service.adjustStock(ID, -4).await().indefinitely();

            assertEquals(6, result.stockQuantity());
        }

        @Test
        @DisplayName("should throw BusinessException when stock is insufficient")
        void shouldThrowWhenInsufficient() {
            when(repository.findById(ID)).thenReturn(Uni.createFrom().item(entity));

            assertThrows(BusinessException.class,
                () -> service.adjustStock(ID, -50).await().indefinitely());

            verify(repository, never()).persist(any(Part.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when missing")
        void shouldThrowWhenMissing() {
            when(repository.findById(ID)).thenReturn(Uni.createFrom().nullItem());

            assertThrows(ResourceNotFoundException.class,
                () -> service.adjustStock(ID, 5).await().indefinitely());
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
