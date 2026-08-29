package br.com.fiap.postech.soat16.fase1.servicecatalog.adapter.in.rest.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceItemResponseDto(
        UUID id,
        String name,
        String description,
        BigDecimal basePrice,
        Integer estimatedDurationMinutes,
        boolean active,
        OffsetDateTime createdAt
) { }
