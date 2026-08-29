package br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.EstimateStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EstimateResponseDto(
    UUID estimateId,
    UUID workOrderId,
    EstimateStatus status,
    BigDecimal partsAmount,
    BigDecimal laborAmount,
    BigDecimal totalAmount,
    LocalDateTime approvedAt,
    LocalDateTime sentAt,
    List<EstimateItemResponseDto> items
) {
    public EstimateResponseDto {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
