package br.com.fiap.postech.soat16.fase1.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import br.com.fiap.postech.soat16.fase1.model.PartType;

public record PartRequestDto(
    @NotBlank(message = "Part name is required") String name,
    String description,
    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero") BigDecimal unitPrice,
    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock cannot be negative") Integer stockQuantity,
    @NotBlank(message = "Unit is required") String unit,
    @Min(value = 0, message = "Minimum stock cannot be negative") Integer minimumStock,
    PartType partType
) {}
