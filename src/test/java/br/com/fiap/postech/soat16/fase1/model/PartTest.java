package br.com.fiap.postech.soat16.fase1.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.exception.BusinessException;

@DisplayName("Part model — Unit Tests")
class PartTest {

    private Part part() {
        return new Part("Filtro de oleo", "Filtro de oleo padrao", new BigDecimal("35.00"), 10, "UN");
    }

    private void setId(Part part, UUID id) {
        try {
            Field field = Part.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(part, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("short constructor defaults minimumStock to 0 and partType to PECA")
        void shortConstructorAppliesDefaults() {
            Part part = part();

            assertEquals(0, part.getMinimumStock());
            assertEquals(PartType.PECA, part.getPartType());
            assertEquals("Filtro de oleo", part.getName());
            assertEquals(new BigDecimal("35.00"), part.getUnitPrice());
            assertEquals(10, part.getStockQuantity());
            assertEquals("UN", part.getUnit());
        }

        @Test
        @DisplayName("full constructor falls back to defaults when minimumStock/partType are null")
        void fullConstructorFallsBackToDefaultsWhenNull() {
            Part part = new Part("Insumo", "desc", BigDecimal.TEN, 5, "UN", null, null);

            assertEquals(0, part.getMinimumStock());
            assertEquals(PartType.PECA, part.getPartType());
        }

        @Test
        @DisplayName("full constructor keeps explicit minimumStock/partType")
        void fullConstructorKeepsExplicitValues() {
            Part part = new Part("Graxa", "desc", BigDecimal.ONE, 100, "L", 20, PartType.INSUMO);

            assertEquals(20, part.getMinimumStock());
            assertEquals(PartType.INSUMO, part.getPartType());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("replaces all mutable fields")
        void replacesAllFields() {
            Part part = part();

            part.update("Novo nome", "Nova desc", new BigDecimal("99.90"), 50, "CX", 5, PartType.INSUMO);

            assertEquals("Novo nome", part.getName());
            assertEquals("Nova desc", part.getDescription());
            assertEquals(new BigDecimal("99.90"), part.getUnitPrice());
            assertEquals(50, part.getStockQuantity());
            assertEquals("CX", part.getUnit());
            assertEquals(5, part.getMinimumStock());
            assertEquals(PartType.INSUMO, part.getPartType());
        }

        @Test
        @DisplayName("falls back to defaults when minimumStock/partType are null")
        void fallsBackToDefaultsWhenNull() {
            Part part = part();

            part.update("Nome", "Desc", BigDecimal.ONE, 1, "UN", null, null);

            assertEquals(0, part.getMinimumStock());
            assertEquals(PartType.PECA, part.getPartType());
        }
    }

    @Nested
    @DisplayName("stock rules")
    class StockRules {

        @Test
        @DisplayName("isLowStock is true when stock is at or below the minimum")
        void isLowStockTrueAtOrBelowMinimum() {
            Part part = new Part("Item", "desc", BigDecimal.ONE, 5, "UN", 5, PartType.PECA);

            assertTrue(part.isLowStock());
        }

        @Test
        @DisplayName("isLowStock is false when stock is above the minimum")
        void isLowStockFalseAboveMinimum() {
            Part part = new Part("Item", "desc", BigDecimal.ONE, 10, "UN", 5, PartType.PECA);

            assertFalse(part.isLowStock());
        }

        @Test
        @DisplayName("decreaseStock subtracts the quantity when stock is sufficient")
        void decreaseStockSubtractsWhenSufficient() {
            Part part = part();

            part.decreaseStock(4);

            assertEquals(6, part.getStockQuantity());
        }

        @Test
        @DisplayName("decreaseStock throws BusinessException when stock is insufficient")
        void decreaseStockThrowsWhenInsufficient() {
            Part part = part();

            BusinessException ex = assertThrows(BusinessException.class, () -> part.decreaseStock(11));

            assertTrue(ex.getMessage().contains("Filtro de oleo"));
            assertEquals(10, part.getStockQuantity());
        }

        @Test
        @DisplayName("increaseStock adds the quantity")
        void increaseStockAddsQuantity() {
            Part part = part();

            part.increaseStock(5);

            assertEquals(15, part.getStockQuantity());
        }
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        @DisplayName("parts are equal when ids match")
        void equalWhenIdsMatch() {
            UUID id = UUID.randomUUID();
            Part a = part();
            Part b = part();
            setId(a, id);
            setId(b, id);

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("parts with different ids are not equal")
        void notEqualWhenIdsDiffer() {
            Part a = part();
            Part b = part();
            setId(a, UUID.randomUUID());
            setId(b, UUID.randomUUID());

            assertNotEquals(a, b);
        }
    }
}
