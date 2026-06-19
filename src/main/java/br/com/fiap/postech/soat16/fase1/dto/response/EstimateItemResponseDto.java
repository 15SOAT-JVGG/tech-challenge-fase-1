package br.com.fiap.postech.soat16.fase1.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EstimateItemResponseDto(
    UUID estimateItemId,
    UUID partId,
    String partName,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal totalPrice
) { }
