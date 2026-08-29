package br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EstimateItemRequestDto(
    @NotNull(message = "partId cannot be blank")
    UUID partId,
    @NotNull(message = "quantity cannot be null")
    @Min(value = 1, message = "quantity must be >= 1")
    Integer quantity,
    @DecimalMin(value = "0.01", message = "unitPrice must be greater than zero")
    BigDecimal unitPrice
) { }
