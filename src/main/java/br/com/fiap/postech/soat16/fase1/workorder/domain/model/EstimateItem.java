package br.com.fiap.postech.soat16.fase1.workorder.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.part.domain.model.Part;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class EstimateItem {

    @EqualsAndHashCode.Include
    private UUID id;

    private Estimate estimate;

    private Part part;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    public static EstimateItem create(Part part, int quantity, BigDecimal requestedUnitPrice) {
        var unitPrice = requestedUnitPrice != null ? requestedUnitPrice : part.getUnitPrice();
        var item = new EstimateItem();
        item.part = part;
        item.quantity = quantity;
        item.unitPrice = unitPrice;
        item.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return item;
    }
}
