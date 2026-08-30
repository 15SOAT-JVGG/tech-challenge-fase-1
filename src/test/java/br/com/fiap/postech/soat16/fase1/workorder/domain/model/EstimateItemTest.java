package br.com.fiap.postech.soat16.fase1.workorder.domain.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.part.domain.model.Part;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.InsufficientPartStockException;

@DisplayName("EstimateItem model — Unit Tests")
class EstimateItemTest {

    @Test
    @DisplayName("computes total from the requested unit price")
    void computesTotalFromRequestedPrice() {
        Part part = new Part("Pastilha", "desc", new BigDecimal("80.00"), 5, "UN");

        EstimateItem item = EstimateItem.create(part, 3, new BigDecimal("50.00"));

        assertEquals(new BigDecimal("50.00"), item.getUnitPrice());
        assertEquals(new BigDecimal("150.00"), item.getTotalPrice());
    }

    @Test
    @DisplayName("uses the catalog price when no unit price is requested")
    void usesCatalogPriceByDefault() {
        Part part = new Part("Oleo", "desc", new BigDecimal("40.00"), 20, "L");

        EstimateItem item = EstimateItem.create(part, 4, null);

        assertEquals(new BigDecimal("40.00"), item.getUnitPrice());
        assertEquals(new BigDecimal("160.00"), item.getTotalPrice());
    }

    @Nested
    @DisplayName("reserva de estoque")
    class StockReservation {

        @Test
        @DisplayName("baixa do saldo da peça a quantidade pedida")
        void reservesRequestedQuantity() {
            Part part = new Part("Filtro", "desc", new BigDecimal("30.00"), 10, "UN");
            EstimateItem item = EstimateItem.create(part, 4, null);

            assertEquals(part, item.reserve());
            assertEquals(6, part.getStockQuantity());
        }

        @Test
        @DisplayName("devolve ao saldo a quantidade reservada")
        void restoresReservedQuantity() {
            Part part = new Part("Filtro", "desc", new BigDecimal("30.00"), 10, "UN");
            EstimateItem item = EstimateItem.create(part, 4, null);
            item.reserve();

            assertEquals(part, item.restore());
            assertEquals(10, part.getStockQuantity());
        }

        @Test
        @DisplayName("aceita a reserva quando o saldo cobre exatamente a quantidade pedida")
        void acceptsExactStock() {
            Part part = new Part("Filtro", "desc", new BigDecimal("30.00"), 4, "UN");
            EstimateItem item = EstimateItem.create(part, 4, null);

            assertDoesNotThrow(item::assertStockAvailable);
        }

        @Test
        @DisplayName("recusa a reserva quando o saldo é insuficiente, sem tocar no estoque")
        void rejectsInsufficientStock() {
            Part part = new Part("Filtro", "desc", new BigDecimal("30.00"), 3, "UN");
            EstimateItem item = EstimateItem.create(part, 4, null);

            assertThrows(InsufficientPartStockException.class, item::assertStockAvailable);
            assertEquals(3, part.getStockQuantity());
        }
    }
}
