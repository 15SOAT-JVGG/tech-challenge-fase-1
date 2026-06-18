package br.com.fiap.postech.soat16.fase1.mapper;

import java.math.BigDecimal;

import org.mapstruct.Mapper;

import br.com.fiap.postech.soat16.fase1.dto.request.EstimateItemRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.EstimateItemResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.EstimateResponseDto;
import br.com.fiap.postech.soat16.fase1.model.Estimate;
import br.com.fiap.postech.soat16.fase1.model.EstimateItem;
import br.com.fiap.postech.soat16.fase1.model.Part;

@Mapper(componentModel = "cdi")
public interface EstimateMapper {

    default EstimateResponseDto toResponse(Estimate entity) {
        if (entity == null) {
            return null;
        }
        return new EstimateResponseDto(
                entity.getId(),
                entity.getWorkOrder().getId(),
                entity.getStatus(),
                entity.getTotalAmount(),
                entity.getApprovedAt(),
                entity.getItems().stream().map(this::toItemResponse).toList()
        );
    }

    default EstimateItemResponseDto toItemResponse(EstimateItem item) {
        return new EstimateItemResponseDto(
                item.getId(),
                item.getPart().getId(),
                item.getPart().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()
        );
    }

    default EstimateItem toItemEntity(EstimateItemRequestDto request, Part part) {
        var unitPrice = request.unitPrice() != null ? request.unitPrice() : part.getUnitPrice();
        var item = new EstimateItem();
        item.setPart(part);
        item.setQuantity(request.quantity());
        item.setUnitPrice(unitPrice);
        item.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(request.quantity())));
        return item;
    }
}
