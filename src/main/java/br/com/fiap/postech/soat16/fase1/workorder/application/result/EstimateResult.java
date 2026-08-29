package br.com.fiap.postech.soat16.fase1.workorder.application.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.EstimateStatus;

public record EstimateResult(
        UUID estimateId,
        UUID workOrderId,
        EstimateStatus status,
        BigDecimal partsAmount,
        BigDecimal laborAmount,
        BigDecimal totalAmount,
        LocalDateTime approvedAt,
        LocalDateTime sentAt,
        List<Item> items
) {

    public EstimateResult {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record Item(
            UUID estimateItemId,
            UUID partId,
            String partName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice
    ) { }
}
