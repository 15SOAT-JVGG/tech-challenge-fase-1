package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.mapper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.reactive.mutiny.Mutiny;

import br.com.fiap.postech.soat16.fase1.part.adapter.out.persistence.entity.PartJpaEntity;
import br.com.fiap.postech.soat16.fase1.part.adapter.out.persistence.mapper.PartPersistenceMapper;
import br.com.fiap.postech.soat16.fase1.shared.adapter.out.persistence.AuditPersistenceMapper;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.entity.EstimateItemJpaEntity;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.entity.EstimateJpaEntity;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.Estimate;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.EstimateItem;

public final class EstimatePersistenceMapper {

    private EstimatePersistenceMapper() {
    }

    public static Estimate toDomain(EstimateJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        var estimate = new Estimate();
        estimate.setId(entity.getId());
        estimate.setWorkOrder(WorkOrderPersistenceMapper.toDomain(entity.getWorkOrder()));
        estimate.setStatus(entity.getStatus());
        estimate.setPartsAmount(entity.getPartsAmount());
        estimate.setLaborAmount(entity.getLaborAmount());
        estimate.setTotalAmount(entity.getTotalAmount());
        estimate.setApprovedAt(entity.getApprovedAt());
        estimate.setSentAt(entity.getSentAt());
        estimate.setReservedAt(entity.getReservedAt());
        AuditPersistenceMapper.copyToDomain(entity, estimate);

        List<EstimateItem> items = new ArrayList<>();
        for (EstimateItemJpaEntity itemEntity : entity.getItems()) {
            items.add(toItemDomain(itemEntity, estimate));
        }
        estimate.setItems(items);
        return estimate;
    }

    /**
     * A peça é resolvida via {@link Mutiny.Session#getReference} porque {@code PartJpaEntity} é
     * versionada: uma instância destacada sem {@code version} seria rejeitada pelo Hibernate ao
     * gravar a chave estrangeira.
     */
    public static EstimateJpaEntity toJpaEntity(Estimate estimate, Mutiny.Session session) {
        if (estimate == null) {
            return null;
        }
        var entity = new EstimateJpaEntity();
        entity.setId(estimate.getId());
        entity.setWorkOrder(WorkOrderPersistenceMapper.toJpaReference(
                estimate.getWorkOrder() != null ? estimate.getWorkOrder().getId() : null));
        copyState(estimate, entity);
        AuditPersistenceMapper.copyToJpaEntity(estimate, entity);

        List<EstimateItemJpaEntity> items = new ArrayList<>();
        for (EstimateItem item : estimate.getItems()) {
            items.add(toItemJpaEntity(item, entity, session));
        }
        entity.setItems(items);
        return entity;
    }

    public static void copyState(Estimate source, EstimateJpaEntity target) {
        target.setStatus(source.getStatus());
        target.setPartsAmount(source.getPartsAmount());
        target.setLaborAmount(source.getLaborAmount());
        target.setTotalAmount(source.getTotalAmount());
        target.setApprovedAt(source.getApprovedAt());
        target.setSentAt(source.getSentAt());
        target.setReservedAt(source.getReservedAt());
    }

    /**
     * Devolve ao domínio os identificadores gerados no insert, inclusive os dos itens em cascata.
     */
    public static void copyGeneratedState(EstimateJpaEntity source, Estimate target) {
        target.setId(source.getId());
        AuditPersistenceMapper.copyToDomain(source, target);
        List<EstimateItemJpaEntity> itemEntities = source.getItems();
        List<EstimateItem> items = target.getItems();
        for (int index = 0; index < items.size() && index < itemEntities.size(); index++) {
            items.get(index).setId(itemEntities.get(index).getId());
        }
    }

    private static EstimateItem toItemDomain(EstimateItemJpaEntity entity, Estimate estimate) {
        var item = new EstimateItem();
        item.setId(entity.getId());
        item.setEstimate(estimate);
        item.setPart(PartPersistenceMapper.toDomain(entity.getPart()));
        item.setQuantity(entity.getQuantity());
        item.setUnitPrice(entity.getUnitPrice());
        item.setTotalPrice(entity.getTotalPrice());
        return item;
    }

    private static EstimateItemJpaEntity toItemJpaEntity(EstimateItem item, EstimateJpaEntity estimate,
            Mutiny.Session session) {
        var entity = new EstimateItemJpaEntity();
        entity.setId(item.getId());
        entity.setEstimate(estimate);
        entity.setPart(item.getPart() != null
                ? session.getReference(PartJpaEntity.class, item.getPart().getId())
                : null);
        entity.setQuantity(item.getQuantity());
        entity.setUnitPrice(item.getUnitPrice());
        entity.setTotalPrice(item.getTotalPrice());
        return entity;
    }
}
