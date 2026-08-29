package br.com.fiap.postech.soat16.fase1.part.application.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.part.domain.model.Part;
import br.com.fiap.postech.soat16.fase1.part.domain.model.enums.PartType;

public record PartResult(
        UUID id,
        String name,
        String description,
        BigDecimal unitPrice,
        Integer stockQuantity,
        String unit,
        Integer minimumStock,
        PartType partType,
        boolean lowStock,
        LocalDateTime createdAt
) {

    public static PartResult from(Part part) {
        return new PartResult(
                part.getId(),
                part.getName(),
                part.getDescription(),
                part.getUnitPrice(),
                part.getStockQuantity(),
                part.getUnit(),
                part.getMinimumStock(),
                part.getPartType(),
                part.isLowStock(),
                part.getCreatedAt());
    }
}
