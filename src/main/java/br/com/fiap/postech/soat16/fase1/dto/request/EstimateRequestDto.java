package br.com.fiap.postech.soat16.fase1.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record EstimateRequestDto(
    @NotEmpty(message = "items cannot be empty")
    @Valid
    List<EstimateItemRequestDto> items
) {
    public EstimateRequestDto {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
