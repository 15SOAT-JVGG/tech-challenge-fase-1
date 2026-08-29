package br.com.fiap.postech.soat16.fase1.part.adapter.in.rest.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.part.domain.model.enums.PartType;

public record PartResponseDto(
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
) { }
