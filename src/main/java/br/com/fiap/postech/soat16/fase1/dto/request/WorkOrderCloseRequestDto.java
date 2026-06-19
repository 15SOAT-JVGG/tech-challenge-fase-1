package br.com.fiap.postech.soat16.fase1.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;

public record WorkOrderCloseRequestDto(
    @DecimalMin(value = "0.01", message = "finalValue must be greater than zero")
    BigDecimal finalValue
) { }
