package br.com.fiap.postech.soat16.fase1.workorder.application.port.out;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.EstimateDecisionToken;

/**
 * Assina e confere os links de decisão. A assinatura só prova a origem do link: quem garante o uso
 * único é o registro do token, recuperado pelo identificador que a conferência devolve.
 */
public interface EstimateDecisionTokenSignaturePort {

    String sign(EstimateDecisionToken token);

    /**
     * @return o identificador do token assinado
     * @throws br.com.fiap.postech.soat16.fase1.workorder.domain.exception.InvalidEstimateDecisionTokenException
     *         quando o link foi adulterado, expirou ou não é um token de decisão
     */
    UUID readTokenId(String signedToken);
}
