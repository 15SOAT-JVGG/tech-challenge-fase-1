package br.com.fiap.postech.soat16.fase1.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkOrderServiceResponseDto(
    UUID workOrderServiceId,
    UUID workOrderId,
    String description,
    BigDecimal price,
    LocalDateTime performedAt
) { }
