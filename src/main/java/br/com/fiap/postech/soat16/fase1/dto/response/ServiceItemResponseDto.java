package br.com.fiap.postech.soat16.fase1.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import br.com.fiap.postech.soat16.fase1.model.ServiceItem;

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
) {
    public static ServiceItemResponseDto from(ServiceItem entity) {
        return new ServiceItemResponseDto(
            entity.getId(), entity.getName(), entity.getDescription(), entity.getBasePrice(),
            entity.getEstimatedDurationMinutes(), entity.isActive(), entity.getCreatedAt()
        );
    }
}
