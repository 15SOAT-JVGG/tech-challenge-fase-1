package br.com.fiap.postech.soat16.fase1.security;

import java.time.Duration;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotAuthorizedException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import br.com.fiap.postech.soat16.fase1.dto.request.LoginRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.LoginResponseDto;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class);
    private static final String INVALID_CREDENTIALS = "Invalid credentials";
    // Dummy hash for constant-time comparison when the user does not exist
    private static final String DUMMY_HASH =
        "$2a$10$7EqJtq98hPqEX7fNZaFWoO9vQKPdN0nW1c6jE8fXE9rH3gYxQ3s2u";

    AppUserRepository userRepository;

    public AuthService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ConfigProperty(name = "app.jwt.expiration-hours", defaultValue = "8")
    long expirationHours;

    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "oficina-api")
    String issuer;

    @WithSession
    public Uni<LoginResponseDto> login(LoginRequestDto request) {
        return userRepository.findByUsername(request.username())
            .onItem().transform(user -> authenticate(request, user));
    }

    private LoginResponseDto authenticate(LoginRequestDto request, AppUser user) {
        // Constant-time comparison avoids timing attacks that leak valid usernames
        boolean validPassword = BcryptUtil.matches(
            request.password(),
            user != null ? user.getPassword() : DUMMY_HASH
        );

        if (user == null || !validPassword) {
            LOG.warnf("Invalid login attempt for user=%s", request.username());
            throw new NotAuthorizedException(INVALID_CREDENTIALS, "Bearer");
        }

        String token = Jwt.issuer(issuer)
            .subject(user.getUsername())
            .groups(Set.of(user.getRole()))
            .expiresIn(Duration.ofHours(expirationHours))
            .sign();

        return new LoginResponseDto(token, user.getUsername(), user.getRole(), expirationHours * 3600);
    }

    @WithTransaction
    public Uni<Void> createUser(String username, String rawPassword, String role) {
        return userRepository.existsByUsername(username)
            .flatMap(exists -> Boolean.TRUE.equals(exists)
                ? Uni.createFrom().failure(
                    new IllegalArgumentException("User already exists: " + username))
                : userRepository.persist(new AppUser(username, BcryptUtil.bcryptHash(rawPassword), role))
                    .replaceWithVoid());
    }
}
