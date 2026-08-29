package br.com.fiap.postech.soat16.fase1.part.application.command;

import java.math.BigDecimal;

import br.com.fiap.postech.soat16.fase1.part.domain.model.enums.PartType;

public record CreatePartCommand(
        String name,
        String description,
        BigDecimal unitPrice,
        Integer stockQuantity,
        String unit,
        Integer minimumStock,
        PartType partType
) { }
