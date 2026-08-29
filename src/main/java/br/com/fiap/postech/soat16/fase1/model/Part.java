package br.com.fiap.postech.soat16.fase1.model;

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

import br.com.fiap.postech.soat16.fase1.exception.BusinessException;
import br.com.fiap.postech.soat16.fase1.model.enums.PartType;

import lombok.EqualsAndHashCode;

@Entity
@Table(name = "parts", schema = "oficina_mecanica")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Part {

    @EqualsAndHashCode.Include
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

    protected Part() {
    }

    public Part(String name, String description, BigDecimal unitPrice, Integer stockQuantity, String unit) {
        this(name, description, unitPrice, stockQuantity, unit, 0, PartType.PART);
    }

    public Part(String name, String description, BigDecimal unitPrice, Integer stockQuantity, String unit,
                Integer minimumStock, PartType partType) {
        this.name = name;
        this.description = description;
        this.unitPrice = unitPrice;
        this.stockQuantity = stockQuantity;
        this.unit = unit;
        this.minimumStock = minimumStock != null ? minimumStock : 0;
        this.partType = partType != null ? partType : PartType.PART;
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void update(String name, String description, BigDecimal unitPrice, Integer stockQuantity, String unit,
                       Integer minimumStock, PartType partType) {
        this.name = name;
        this.description = description;
        this.unitPrice = unitPrice;
        this.stockQuantity = stockQuantity;
        this.unit = unit;
        this.minimumStock = minimumStock != null ? minimumStock : 0;
        this.partType = partType != null ? partType : PartType.PART;
    }

    public boolean isLowStock() {
        return minimumStock != null && stockQuantity <= minimumStock;
    }

    public void decreaseStock(int quantity) {
        if (this.stockQuantity < quantity) {
            throw new BusinessException(
                "Insufficient stock for part '" + name + "'. Available: " + stockQuantity
            );
        }
        this.stockQuantity -= quantity;
    }

    public void increaseStock(int quantity) {
        this.stockQuantity += quantity;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Integer getStockQuantity() { return stockQuantity; }
    public String getUnit() { return unit; }
    public Integer getMinimumStock() { return minimumStock; }
    public PartType getPartType() { return partType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
