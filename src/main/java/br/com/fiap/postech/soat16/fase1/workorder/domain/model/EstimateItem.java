package br.com.fiap.postech.soat16.fase1.workorder.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import br.com.fiap.postech.soat16.fase1.model.Part;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "estimate_item", schema = "oficina_mecanica")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class EstimateItem {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "estimate_item_id", nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "estimate_id", nullable = false)
    private Estimate estimate;

    @ManyToOne
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
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
