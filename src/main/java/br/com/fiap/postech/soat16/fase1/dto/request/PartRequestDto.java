package br.com.fiap.postech.soat16.fase1.dto.request;

import br.com.fiap.postech.soat16.fase1.model.PartType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PartRequestDto(
    @NotBlank(message = "Nome da peça é obrigatório") String name,
    String description,
    @NotNull(message = "Preço unitário é obrigatório")
    @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero") BigDecimal unitPrice,
    @NotNull(message = "Quantidade em estoque é obrigatória")
    @Min(value = 0, message = "Estoque não pode ser negativo") Integer stockQuantity,
    @NotBlank(message = "Unidade é obrigatória") String unit,
    @Min(value = 0, message = "Estoque mínimo não pode ser negativo") Integer minimumStock,
    PartType partType
) {}
