package br.com.fiap.postech.soat16.fase1.workorder.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record AddWorkOrderServiceCommand(
        String description,
        BigDecimal price,
        UUID serviceItemId
) { }
