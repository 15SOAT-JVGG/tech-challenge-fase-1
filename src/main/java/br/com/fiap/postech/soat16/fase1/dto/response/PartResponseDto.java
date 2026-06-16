package br.com.fiap.postech.soat16.fase1.dto.response;

import br.com.fiap.postech.soat16.fase1.model.Part;
import br.com.fiap.postech.soat16.fase1.model.PartType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PartResponseDto(
    Long id,
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
    public static PartResponseDto from(Part p) {
        return new PartResponseDto(
            p.getId(), p.getName(), p.getDescription(), p.getUnitPrice(),
            p.getStockQuantity(), p.getUnit(), p.getMinimumStock(), p.getPartType(),
            p.isLowStock(), p.getCreatedAt()
        );
    }
}
