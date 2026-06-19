package br.com.fiap.postech.soat16.fase1.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import br.com.fiap.postech.soat16.fase1.model.EstimateStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EstimateResponseDto(
    UUID estimateId,
    UUID workOrderId,
    EstimateStatus status,
    BigDecimal totalAmount,
    LocalDateTime approvedAt,
    List<EstimateItemResponseDto> items
) { }
