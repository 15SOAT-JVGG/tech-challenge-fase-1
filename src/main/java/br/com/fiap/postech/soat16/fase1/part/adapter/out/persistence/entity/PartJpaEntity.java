package br.com.fiap.postech.soat16.fase1.part.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import br.com.fiap.postech.soat16.fase1.part.domain.model.enums.PartType;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "Part")
@Table(name = "parts", schema = "oficina_mecanica")
@Getter
@Setter
public class PartJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "part_id", nullable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column
    private String description;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(nullable = false, length = 10)
    private String unit;

    @Column(name = "minimum_stock", nullable = false)
    private Integer minimumStock = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "part_type", nullable = false, length = 10)
    private PartType partType = PartType.PART;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
