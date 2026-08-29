package br.com.fiap.postech.soat16.fase1.part.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.part.domain.model.enums.PartType;
import br.com.fiap.postech.soat16.fase1.shared.domain.exception.BusinessException;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Part {

    @EqualsAndHashCode.Include
    private UUID id;

    private String name;

    private String description;

    private BigDecimal unitPrice;

    private Integer stockQuantity;

    private String unit;

    private Integer minimumStock = 0;

    private PartType partType = PartType.PART;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    protected Part() {
    }

    public Part(
            String name,
            String description,
            BigDecimal unitPrice,
            Integer stockQuantity,
            String unit) {
        this(name, description, unitPrice, stockQuantity, unit, 0, PartType.PART);
    }

    public Part(
            String name,
            String description,
            BigDecimal unitPrice,
            Integer stockQuantity,
            String unit,
            Integer minimumStock,
            PartType partType) {
        this.name = name;
        this.description = description;
        this.unitPrice = unitPrice;
        this.stockQuantity = stockQuantity;
        this.unit = unit;
        this.minimumStock = minimumStock != null ? minimumStock : 0;
        this.partType = partType != null ? partType : PartType.PART;
    }

    /**
     * Reidrata uma peça já persistida, preservando identidade e datas geradas pela infraestrutura.
     */
    public static Part restore(
            UUID id,
            String name,
            String description,
            BigDecimal unitPrice,
            Integer stockQuantity,
            String unit,
            Integer minimumStock,
            PartType partType,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        var part = new Part(name, description, unitPrice, stockQuantity, unit, minimumStock, partType);
        part.id = id;
        part.createdAt = createdAt;
        part.updatedAt = updatedAt;
        return part;
    }

    public void update(
            String name,
            String description,
            BigDecimal unitPrice,
            Integer stockQuantity,
            String unit,
            Integer minimumStock,
            PartType partType) {
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
        if (stockQuantity < quantity) {
            throw new BusinessException(
                    "Insufficient stock for part '" + name + "'. Available: " + stockQuantity);
        }
        stockQuantity -= quantity;
    }

    public void increaseStock(int quantity) {
        stockQuantity += quantity;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public String getUnit() {
        return unit;
    }

    public Integer getMinimumStock() {
        return minimumStock;
    }

    public PartType getPartType() {
        return partType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
