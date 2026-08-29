package br.com.fiap.postech.soat16.fase1.workorder.application.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateEstimateCommand(List<Item> items) {

    public CreateEstimateCommand {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record Item(UUID partId, int quantity, BigDecimal unitPrice) { }
}
