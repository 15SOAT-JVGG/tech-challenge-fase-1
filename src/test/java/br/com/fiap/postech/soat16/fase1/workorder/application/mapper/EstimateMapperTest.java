package br.com.fiap.postech.soat16.fase1.workorder.application.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.model.Part;
import br.com.fiap.postech.soat16.fase1.workorder.application.result.EstimateResult;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.Estimate;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.EstimateItem;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrder;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.EstimateStatus;

@DisplayName("EstimateMapper — Unit Tests")
class EstimateMapperTest {

    private final EstimateMapper mapper = new EstimateMapper() { };

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("should map estimate and its items")
        void shouldMapEstimateAndItems() {
            UUID estimateId = UUID.randomUUID();
            UUID workOrderId = UUID.randomUUID();
            UUID partId = UUID.randomUUID();

            WorkOrder workOrder = new WorkOrder();
            workOrder.setId(workOrderId);

            Part part = new Part("Filtro de oleo", "desc", BigDecimal.valueOf(30), 10, "UN");
            setPartId(part, partId);

            Estimate estimate = new Estimate();
            estimate.setId(estimateId);
            estimate.setWorkOrder(workOrder);
            estimate.setStatus(EstimateStatus.PENDING);
            estimate.setTotalAmount(BigDecimal.valueOf(60));

            EstimateItem item = new EstimateItem();
            item.setEstimate(estimate);
            item.setPart(part);
            item.setQuantity(2);
            item.setUnitPrice(BigDecimal.valueOf(30));
            item.setTotalPrice(BigDecimal.valueOf(60));
            estimate.setItems(List.of(item));

            EstimateResult result = mapper.toResult(estimate);

            assertNotNull(result);
            assertEquals(estimateId, result.estimateId());
            assertEquals(workOrderId, result.workOrderId());
            assertEquals(EstimateStatus.PENDING, result.status());
            assertEquals(BigDecimal.valueOf(60), result.totalAmount());
            assertEquals(1, result.items().size());
            assertEquals(partId, result.items().getFirst().partId());
            assertEquals("Filtro de oleo", result.items().getFirst().partName());
            assertEquals(2, result.items().getFirst().quantity());
            assertEquals(BigDecimal.valueOf(60), result.items().getFirst().totalPrice());
        }
    }

    private void setPartId(Part part, UUID id) {
        try {
            var field = Part.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(part, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
