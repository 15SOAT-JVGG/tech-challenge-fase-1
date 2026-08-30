package br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record InitialServiceRequestDto(
    @NotNull(message = "serviceItemId cannot be null")
    UUID serviceItemId
) { }
