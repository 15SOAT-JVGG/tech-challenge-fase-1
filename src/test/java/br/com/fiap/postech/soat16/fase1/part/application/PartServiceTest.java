package br.com.fiap.postech.soat16.fase1.part.application;

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

import br.com.fiap.postech.soat16.fase1.part.application.command.AdjustPartStockCommand;
import br.com.fiap.postech.soat16.fase1.part.application.command.CreatePartCommand;
import br.com.fiap.postech.soat16.fase1.part.application.command.DeletePartCommand;
import br.com.fiap.postech.soat16.fase1.part.application.command.FindPartQuery;
import br.com.fiap.postech.soat16.fase1.part.application.command.UpdatePartCommand;
import br.com.fiap.postech.soat16.fase1.part.application.port.out.PartPersistencePort;
import br.com.fiap.postech.soat16.fase1.part.application.result.PartResult;
import br.com.fiap.postech.soat16.fase1.part.domain.model.Part;
import br.com.fiap.postech.soat16.fase1.part.domain.model.enums.PartType;
import br.com.fiap.postech.soat16.fase1.shared.domain.exception.BusinessException;
import br.com.fiap.postech.soat16.fase1.shared.domain.exception.ResourceNotFoundException;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartService — Unit Tests")
class PartServiceTest {

    private static final UUID ID =
            UUID.fromString("c3b79cde-2872-4053-9622-37605bf124a3");

    @Mock
    private PartPersistencePort partPersistence;

    private PartService service;
    private Part entity;

    @BeforeEach
    void setUp() {
        service = new PartService(partPersistence);
        entity = new Part(
                "Óleo 5W30",
                "Óleo sintético",
                new BigDecimal("49.90"),
                10,
                "L",
                5,
                PartType.SUPPLY);
    }

    private CreatePartCommand createCommand(int stockQuantity) {
        return new CreatePartCommand(
                "Óleo 5W30",
                "Óleo sintético",
                new BigDecimal("49.90"),
                stockQuantity,
                "L",
                5,
                PartType.SUPPLY);
    }

    private UpdatePartCommand updateCommand(int stockQuantity) {
        return new UpdatePartCommand(
                ID,
                "Óleo 5W30",
                "Óleo sintético",
                new BigDecimal("49.90"),
                stockQuantity,
                "L",
                5,
                PartType.SUPPLY);
    }

    @Nested
    @DisplayName("listAll")
    class ListAll {

        @Test
        @DisplayName("should map all parts to result")
        void shouldMapAll() {
            when(partPersistence.listAllParts())
                    .thenReturn(Uni.createFrom().item(List.of(entity)));

            List<PartResult> result = service.listAll().await().indefinitely();

            assertEquals(1, result.size());
            assertEquals("Óleo 5W30", result.getFirst().name());
        }

        @Test
        @DisplayName("should return empty list when no parts exist")
        void shouldReturnEmpty() {
            when(partPersistence.listAllParts())
                    .thenReturn(Uni.createFrom().item(List.of()));

            assertTrue(service.listAll().await().indefinitely().isEmpty());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return part when found")
        void shouldReturnWhenFound() {
            when(partPersistence.findPartById(ID))
                    .thenReturn(Uni.createFrom().item(entity));

            PartResult result = service.findById(new FindPartQuery(ID)).await().indefinitely();

            assertNotNull(result);
            assertEquals("Óleo 5W30", result.name());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when missing")
        void shouldThrowWhenMissing() {
            when(partPersistence.findPartById(ID))
                    .thenReturn(Uni.createFrom().nullItem());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> service.findById(new FindPartQuery(ID)).await().indefinitely());
        }
    }

    @Test
    @DisplayName("findLowStock should map low-stock parts to result")
    void findLowStockShouldMap() {
        when(partPersistence.findLowStock())
                .thenReturn(Uni.createFrom().item(List.of(entity)));

        List<PartResult> result = service.findLowStock().await().indefinitely();

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("create should persist part and return result")
    void createShouldPersist() {
        when(partPersistence.save(any(Part.class)))
                .thenReturn(Uni.createFrom().item(entity));

        PartResult result = service.create(createCommand(10)).await().indefinitely();

        assertNotNull(result);
        verify(partPersistence).save(any(Part.class));
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update entity and return result")
        void shouldUpdate() {
            when(partPersistence.findPartById(ID))
                    .thenReturn(Uni.createFrom().item(entity));
            when(partPersistence.save(entity))
                    .thenReturn(Uni.createFrom().item(entity));

            PartResult result = service.update(updateCommand(20)).await().indefinitely();

            assertNotNull(result);
            assertEquals(20, entity.getStockQuantity());
            verify(partPersistence).save(entity);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when missing")
        void shouldThrowWhenMissing() {
            when(partPersistence.findPartById(ID))
                    .thenReturn(Uni.createFrom().nullItem());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> service.update(updateCommand(20)).await().indefinitely());
            verify(partPersistence, never()).save(any(Part.class));
        }
    }

    @Nested
    @DisplayName("adjustStock")
    class AdjustStock {

        @Test
        @DisplayName("should increase stock on positive adjustment")
        void shouldIncrease() {
            prepareExistingPart();

            PartResult result = service.adjustStock(new AdjustPartStockCommand(ID, 5))
                    .await().indefinitely();

            assertEquals(15, result.stockQuantity());
        }

        @Test
        @DisplayName("should decrease stock on negative adjustment")
        void shouldDecrease() {
            prepareExistingPart();

            PartResult result = service.adjustStock(new AdjustPartStockCommand(ID, -4))
                    .await().indefinitely();

            assertEquals(6, result.stockQuantity());
        }

        @Test
        @DisplayName("should throw BusinessException when stock is insufficient")
        void shouldThrowWhenInsufficient() {
            when(partPersistence.findPartById(ID))
                    .thenReturn(Uni.createFrom().item(entity));

            assertThrows(
                    BusinessException.class,
                    () -> service.adjustStock(new AdjustPartStockCommand(ID, -50))
                            .await().indefinitely());
            verify(partPersistence, never()).save(any(Part.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when missing")
        void shouldThrowWhenMissing() {
            when(partPersistence.findPartById(ID))
                    .thenReturn(Uni.createFrom().nullItem());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> service.adjustStock(new AdjustPartStockCommand(ID, 5))
                            .await().indefinitely());
        }

        private void prepareExistingPart() {
            when(partPersistence.findPartById(ID))
                    .thenReturn(Uni.createFrom().item(entity));
            when(partPersistence.save(entity))
                    .thenReturn(Uni.createFrom().item(entity));
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete when record exists")
        void shouldDelete() {
            when(partPersistence.deletePartById(ID))
                    .thenReturn(Uni.createFrom().item(true));

            assertDoesNotThrow(
                    () -> service.delete(new DeletePartCommand(ID)).await().indefinitely());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when nothing was deleted")
        void shouldThrowWhenNothingDeleted() {
            when(partPersistence.deletePartById(ID))
                    .thenReturn(Uni.createFrom().item(false));

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> service.delete(new DeletePartCommand(ID)).await().indefinitely());
        }
    }
}
