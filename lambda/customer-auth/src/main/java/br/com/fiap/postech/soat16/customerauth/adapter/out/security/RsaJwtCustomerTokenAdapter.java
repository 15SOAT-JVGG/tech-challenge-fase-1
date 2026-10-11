package br.com.fiap.postech.soat16.customerauth.adapter.out.security;

import java.security.interfaces.RSAPrivateKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import br.com.fiap.postech.soat16.customerauth.application.IssuedToken;
import br.com.fiap.postech.soat16.customerauth.application.port.out.CustomerTokenPort;
import br.com.fiap.postech.soat16.customerauth.domain.Customer;

/**
 * Emite o token no contrato que a API já valida (MicroProfile JWT): assinado com a mesma chave
 * privada RS256, com o issuer que a API espera, o id do cliente em {@code sub} e o papel em
 * {@code groups}. A API não sabe — nem precisa saber — que o token não saiu dela.
 */
public class RsaJwtCustomerTokenAdapter implements CustomerTokenPort {

    public static final String CUSTOMER_ROLE = "CUSTOMER";

    private final RSASSASigner signer;
    private final String issuer;
    private final Duration lifetime;
    private final Clock clock;

    public RsaJwtCustomerTokenAdapter(RSAPrivateKey privateKey, String issuer, Duration lifetime, Clock clock) {
        this.signer = new RSASSASigner(privateKey);
        this.issuer = issuer;
        this.lifetime = lifetime;
        this.clock = clock;
    }

    @Override
    public IssuedToken issueFor(Customer customer) {
        Instant issuedAt = clock.instant();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(customer.id().toString())
                .claim("groups", List.of(CUSTOMER_ROLE))
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(issuedAt.plus(lifetime)))
                .build();
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build();

        SignedJWT jwt = new SignedJWT(header, claims);
        try {
            jwt.sign(signer);
        } catch (JOSEException exception) {
            throw new IllegalStateException("Falha ao assinar o token do cliente", exception);
        }
        return new IssuedToken(jwt.serialize(), customer.id(), CUSTOMER_ROLE, lifetime.toSeconds());
    }
}
