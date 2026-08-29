package br.com.fiap.postech.soat16.fase1.workorder.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.part.domain.model.Part;

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
}
