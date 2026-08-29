package br.com.fiap.postech.soat16.fase1.workorder.application.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WorkOrderServiceResult(
        UUID workOrderServiceId,
        UUID workOrderId,
        String description,
        BigDecimal price,
        LocalDateTime performedAt,
        UUID serviceItemId
) { }
