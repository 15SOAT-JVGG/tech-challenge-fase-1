package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.security;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import br.com.fiap.postech.soat16.fase1.workorder.application.port.out.WorkOrderTrackingTokenSignaturePort;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.InvalidWorkOrderTrackingTokenException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrderTrackingToken;

import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.jwt.build.Jwt;

/**
 * Assina os links de acompanhamento com o mesmo par RS256 da API. A ordem de serviço viaja no
 * {@code sub} e a emissão no {@code iat}, de onde o domínio deriva o prazo de trinta dias.
 *
 * <p>Como no link de decisão, o {@code exp} do JWT é deliberadamente muito mais longo que a janela
 * de acompanhamento: um link vencido precisa ser lido para responder "expirado", e não morrer antes
 * disso como se fosse forjado.
 */
@ApplicationScoped
public class JwtWorkOrderTrackingTokenAdapter implements WorkOrderTrackingTokenSignaturePort {

    private static final String PURPOSE_CLAIM = "purpose";
    private static final String WORK_ORDER_TRACKING_PURPOSE = "work-order-tracking";
    private static final Duration SIGNATURE_MARGIN = Duration.ofDays(365);

    private final String issuer;
    private final JWTParser parser;

    public JwtWorkOrderTrackingTokenAdapter(
            @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "oficina-api")
            String issuer,
            JWTParser parser) {
        this.issuer = issuer;
        this.parser = parser;
    }

    @Override
    public String sign(WorkOrderTrackingToken token) {
        return Jwt.issuer(issuer)
                .subject(token.workOrderId().toString())
                .claim(PURPOSE_CLAIM, WORK_ORDER_TRACKING_PURPOSE)
                .issuedAt(toEpochSeconds(token.issuedAt()))
                .expiresAt(toEpochSeconds(token.expiresAt().plus(SIGNATURE_MARGIN)))
                .sign();
    }

    @Override
    public WorkOrderTrackingToken read(String signedToken) {
        try {
            var jwt = parser.parse(signedToken);
            if (!issuedForTracking(jwt)) {
                throw new InvalidWorkOrderTrackingTokenException();
            }
            return WorkOrderTrackingToken.issue(
                    UUID.fromString(jwt.getSubject()),
                    toLocalDateTime(jwt.getIssuedAtTime()));
        } catch (ParseException | IllegalArgumentException exception) {
            throw new InvalidWorkOrderTrackingTokenException(exception);
        }
    }

    /**
     * Além do propósito, o token precisa trazer a ordem e a data de emissão: sem elas não há o que
     * consultar nem prazo a conferir, e o link é tão inútil quanto um forjado.
     */
    private static boolean issuedForTracking(JsonWebToken jwt) {
        return WORK_ORDER_TRACKING_PURPOSE.equals(jwt.getClaim(PURPOSE_CLAIM))
                && jwt.getSubject() != null
                && jwt.getIssuedAtTime() != 0;
    }

    private static long toEpochSeconds(LocalDateTime moment) {
        return moment.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private static LocalDateTime toLocalDateTime(long epochSeconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
    }
}
