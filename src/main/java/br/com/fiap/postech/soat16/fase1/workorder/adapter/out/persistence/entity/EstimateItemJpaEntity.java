package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.entity;

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

import br.com.fiap.postech.soat16.fase1.part.adapter.out.persistence.entity.PartJpaEntity;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "EstimateItem")
@Table(name = "estimate_item", schema = "oficina_mecanica")
@Getter
@Setter
public class EstimateItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "estimate_item_id", nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "estimate_id", nullable = false)
    private EstimateJpaEntity estimate;

    @ManyToOne
    @JoinColumn(name = "part_id", nullable = false)
    private PartJpaEntity part;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;
}
