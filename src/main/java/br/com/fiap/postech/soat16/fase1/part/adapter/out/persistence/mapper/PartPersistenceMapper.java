package br.com.fiap.postech.soat16.fase1.part.adapter.out.persistence.mapper;

import br.com.fiap.postech.soat16.fase1.part.adapter.out.persistence.entity.PartJpaEntity;
import br.com.fiap.postech.soat16.fase1.part.domain.model.Part;

public final class PartPersistenceMapper {

    private PartPersistenceMapper() {
    }

    public static Part toDomain(PartJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Part.restore(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getUnitPrice(),
                entity.getStockQuantity(),
                entity.getUnit(),
                entity.getMinimumStock(),
                entity.getPartType(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static PartJpaEntity toJpaEntity(Part part) {
        if (part == null) {
            return null;
        }
        var entity = new PartJpaEntity();
        entity.setId(part.getId());
        entity.setCreatedAt(part.getCreatedAt());
        entity.setUpdatedAt(part.getUpdatedAt());
        copyState(part, entity);
        return entity;
    }

    public static void copyState(Part source, PartJpaEntity target) {
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setUnitPrice(source.getUnitPrice());
        target.setStockQuantity(source.getStockQuantity());
        target.setUnit(source.getUnit());
        target.setMinimumStock(source.getMinimumStock());
        target.setPartType(source.getPartType());
    }
}
