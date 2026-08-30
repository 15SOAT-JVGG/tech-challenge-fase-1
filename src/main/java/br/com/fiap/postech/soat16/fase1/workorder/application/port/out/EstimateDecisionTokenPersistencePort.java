package br.com.fiap.postech.soat16.fase1.workorder.application.port.out;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.EstimateDecisionToken;

import io.smallrye.mutiny.Uni;

public interface EstimateDecisionTokenPersistencePort {

    Uni<EstimateDecisionToken> findByTokenId(UUID tokenId);

    Uni<EstimateDecisionToken> save(EstimateDecisionToken token);
}
