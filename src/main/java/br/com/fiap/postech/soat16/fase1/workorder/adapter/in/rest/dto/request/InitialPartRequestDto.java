package br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InitialPartRequestDto(
    @NotNull(message = "partId cannot be null")
    UUID partId,
    @NotNull(message = "quantity cannot be null")
    @Min(value = 1, message = "quantity must be >= 1")
    Integer quantity
) { }
