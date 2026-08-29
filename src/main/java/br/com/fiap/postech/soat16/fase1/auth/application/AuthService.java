package br.com.fiap.postech.soat16.fase1.auth.application;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import br.com.fiap.postech.soat16.fase1.auth.application.command.LoginCommand;
import br.com.fiap.postech.soat16.fase1.auth.application.port.out.AppUserPersistencePort;
import br.com.fiap.postech.soat16.fase1.auth.application.port.out.AuthPasswordPort;
import br.com.fiap.postech.soat16.fase1.auth.application.port.out.JwtTokenPort;
import br.com.fiap.postech.soat16.fase1.auth.application.result.IssuedToken;
import br.com.fiap.postech.soat16.fase1.auth.application.result.LoginResult;
import br.com.fiap.postech.soat16.fase1.auth.domain.exception.InvalidCredentialsException;
import br.com.fiap.postech.soat16.fase1.auth.domain.model.AppUser;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class);
    private static final String DUMMY_HASH =
        "$2a$10$7EqJtq98hPqEX7fNZaFWoO9vQKPdN0nW1c6jE8fXE9rH3gYxQ3s2u";

    private final AppUserPersistencePort userRepository;
    private final AuthPasswordPort password;
    private final JwtTokenPort token;

    public AuthService(
            AppUserPersistencePort userRepository,
            AuthPasswordPort password,
            JwtTokenPort token
    ) {
        this.userRepository = userRepository;
        this.password = password;
        this.token = token;
    }

    @WithSession
    public Uni<LoginResult> login(LoginCommand command) {
        return userRepository.findByUsername(command.username())
                .onItem().transform(user -> authenticate(command, user));
    }

    @WithSession
    public Uni<Boolean> hasActiveUser(String username) {
        return userRepository.findByUsername(username).map(user -> user != null);
    }

    @WithTransaction
    public Uni<Void> createUser(String username, String rawPassword, String role) {
        return userRepository.existsByUsername(username)
                .flatMap(exists -> Boolean.TRUE.equals(exists)
                        ? Uni.createFrom().failure(
                                new IllegalArgumentException("User already exists: " + username))
                        : userRepository.save(new AppUser(username, password.hash(rawPassword), role))
                                .replaceWithVoid());
    }

    private LoginResult authenticate(LoginCommand command, AppUser user) {
        boolean validPassword = password.matches(
                command.password(),
                user != null ? user.getPassword() : DUMMY_HASH);

        if (user == null || !validPassword) {
            LOG.warnf("Invalid login attempt for user=%s", command.username());
            throw new InvalidCredentialsException();
        }

        IssuedToken issuedToken = token.issue(user.getUsername(), user.getRole());
        return new LoginResult(
                issuedToken.token(),
                user.getUsername(),
                user.getRole(),
                issuedToken.expiresIn());
    }
}
