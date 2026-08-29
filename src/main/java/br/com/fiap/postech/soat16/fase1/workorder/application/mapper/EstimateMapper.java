package br.com.fiap.postech.soat16.fase1.workorder.application.mapper;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.workorder.application.result.EstimateResult;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.Estimate;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.EstimateItem;

@ApplicationScoped
public class EstimateMapper {

    public EstimateResult toResult(Estimate entity) {
        if (entity == null) {
            return null;
        }
        return new EstimateResult(
                entity.getId(),
                entity.getWorkOrder().getId(),
                entity.getStatus(),
                entity.getPartsAmount(),
                entity.getLaborAmount(),
                entity.getTotalAmount(),
                entity.getApprovedAt(),
                entity.getSentAt(),
                entity.getItems().stream().map(this::toItemResult).toList()
        );
    }

    public EstimateResult.Item toItemResult(EstimateItem item) {
        return new EstimateResult.Item(
                item.getId(),
                item.getPart().getId(),
                item.getPart().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()
        );
    }

}
