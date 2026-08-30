package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.entity.EstimateDecisionTokenJpaEntity;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.mapper.EstimateDecisionTokenPersistenceMapper;
import br.com.fiap.postech.soat16.fase1.workorder.application.port.out.EstimateDecisionTokenPersistencePort;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.EstimateDecisionToken;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class EstimateDecisionTokenRepository
        implements PanacheRepositoryBase<EstimateDecisionTokenJpaEntity, UUID>,
        EstimateDecisionTokenPersistencePort {

    @Override
    public Uni<EstimateDecisionToken> findByTokenId(UUID tokenId) {
        return findById(tokenId).map(EstimateDecisionTokenPersistenceMapper::toDomain);
    }

    @Override
    public Uni<EstimateDecisionToken> save(EstimateDecisionToken token) {
        return findById(token.getId())
                .flatMap(existing -> {
                    if (existing == null) {
                        return persist(EstimateDecisionTokenPersistenceMapper.toJpaEntity(token));
                    }
                    EstimateDecisionTokenPersistenceMapper.copyState(token, existing);
                    return Uni.createFrom().item(existing);
                })
                .replaceWith(token);
    }
}
