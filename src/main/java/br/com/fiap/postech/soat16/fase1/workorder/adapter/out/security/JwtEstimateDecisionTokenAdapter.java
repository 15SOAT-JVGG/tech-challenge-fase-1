package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.security;

import java.time.Duration;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import br.com.fiap.postech.soat16.fase1.workorder.application.port.out.EstimateDecisionTokenSignaturePort;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.InvalidEstimateDecisionTokenException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.EstimateDecisionToken;

import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.jwt.build.Jwt;

/**
 * Assina os links de decisão com o mesmo par RS256 da API. A assinatura prova apenas que o link
 * saiu da oficina e não foi adulterado; ela não decide se a janela de sete dias já passou.
 *
 * <p>Por isso o {@code exp} do JWT é deliberadamente muito mais longo que a janela de decisão: um
 * link vencido precisa chegar ao registro do token para responder "expirado", e não morrer antes
 * disso como se fosse forjado. A margem é de um ano porque um cliente que guardou o e-mail por
 * meses ainda merece ouvir que o prazo passou, e não que o link é inválido.
 */
@ApplicationScoped
public class JwtEstimateDecisionTokenAdapter implements EstimateDecisionTokenSignaturePort {

    private static final String PURPOSE_CLAIM = "purpose";
    private static final String ESTIMATE_DECISION_PURPOSE = "estimate-decision";
    private static final Duration SIGNATURE_MARGIN = Duration.ofDays(365);

    private final String issuer;
    private final JWTParser parser;

    public JwtEstimateDecisionTokenAdapter(
            @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "oficina-api")
            String issuer,
            JWTParser parser) {
        this.issuer = issuer;
        this.parser = parser;
    }

    @Override
    public String sign(EstimateDecisionToken token) {
        return Jwt.issuer(issuer)
                .subject(token.getId().toString())
                .claim(PURPOSE_CLAIM, ESTIMATE_DECISION_PURPOSE)
                .expiresIn(EstimateDecisionToken.DECISION_WINDOW.plus(SIGNATURE_MARGIN))
                .sign();
    }

    @Override
    public UUID readTokenId(String signedToken) {
        try {
            var jwt = parser.parse(signedToken);
            if (!ESTIMATE_DECISION_PURPOSE.equals(jwt.getClaim(PURPOSE_CLAIM))) {
                throw new InvalidEstimateDecisionTokenException();
            }
            return UUID.fromString(jwt.getSubject());
        } catch (ParseException | IllegalArgumentException exception) {
            throw new InvalidEstimateDecisionTokenException(exception);
        }
    }
}
