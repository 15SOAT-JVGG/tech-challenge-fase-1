package br.com.fiap.postech.soat16.fase1.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkOrderServiceRequestDto(
    @NotBlank(message = "description cannot be blank")
    String description,
    @NotNull(message = "price cannot be null")
    @DecimalMin(value = "0.01", message = "price must be greater than zero")
    BigDecimal price
) { }
