package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.mapper;

import br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.entity.EstimateDecisionTokenJpaEntity;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.EstimateDecisionToken;

public final class EstimateDecisionTokenPersistenceMapper {

    private EstimateDecisionTokenPersistenceMapper() {
    }

    public static EstimateDecisionToken toDomain(EstimateDecisionTokenJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        var token = new EstimateDecisionToken();
        token.setId(entity.getId());
        token.setWorkOrderId(entity.getWorkOrderId());
        token.setEstimateId(entity.getEstimateId());
        token.setDecision(entity.getDecision());
        token.setIssuedAt(entity.getIssuedAt());
        token.setExpiresAt(entity.getExpiresAt());
        token.setConsumedAt(entity.getConsumedAt());
        return token;
    }

    public static EstimateDecisionTokenJpaEntity toJpaEntity(EstimateDecisionToken token) {
        if (token == null) {
            return null;
        }
        var entity = new EstimateDecisionTokenJpaEntity();
        entity.setId(token.getId());
        entity.setWorkOrderId(token.getWorkOrderId());
        entity.setEstimateId(token.getEstimateId());
        entity.setIssuedAt(token.getIssuedAt());
        copyState(token, entity);
        return entity;
    }

    public static void copyState(EstimateDecisionToken source, EstimateDecisionTokenJpaEntity target) {
        target.setDecision(source.getDecision());
        target.setExpiresAt(source.getExpiresAt());
        target.setConsumedAt(source.getConsumedAt());
    }
}
