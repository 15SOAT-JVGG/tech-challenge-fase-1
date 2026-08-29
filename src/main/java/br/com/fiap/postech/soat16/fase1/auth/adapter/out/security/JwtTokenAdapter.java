package br.com.fiap.postech.soat16.fase1.auth.adapter.out.security;

import java.time.Duration;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import br.com.fiap.postech.soat16.fase1.auth.application.port.out.JwtTokenPort;
import br.com.fiap.postech.soat16.fase1.auth.application.result.IssuedToken;

import io.smallrye.jwt.build.Jwt;

@ApplicationScoped
public class JwtTokenAdapter implements JwtTokenPort {

    private final long expirationHours;
    private final String issuer;

    public JwtTokenAdapter(
            @ConfigProperty(name = "app.jwt.expiration-hours", defaultValue = "8")
            long expirationHours,
            @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "oficina-api")
            String issuer
    ) {
        this.expirationHours = expirationHours;
        this.issuer = issuer;
    }

    @Override
    public IssuedToken issue(String subject, String role) {
        String token = Jwt.issuer(issuer)
                .subject(subject)
                .groups(Set.of(role))
                .expiresIn(Duration.ofHours(expirationHours))
                .sign();
        return new IssuedToken(token, expirationHours * 3600);
    }
}
