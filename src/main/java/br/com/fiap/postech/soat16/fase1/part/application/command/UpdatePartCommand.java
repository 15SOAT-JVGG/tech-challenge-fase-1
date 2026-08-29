package br.com.fiap.postech.soat16.fase1.part.application.command;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.part.domain.model.enums.PartType;

public record UpdatePartCommand(
        UUID id,
        String name,
        String description,
        BigDecimal unitPrice,
        Integer stockQuantity,
        String unit,
        Integer minimumStock,
        PartType partType
) { }
