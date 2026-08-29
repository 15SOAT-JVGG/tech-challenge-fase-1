package br.com.fiap.postech.soat16.fase1.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.fiap.postech.soat16.fase1.auth.adapter.out.persistence.AppUserRepository;
import br.com.fiap.postech.soat16.fase1.auth.application.command.LoginCommand;
import br.com.fiap.postech.soat16.fase1.auth.domain.exception.InvalidCredentialsException;
import br.com.fiap.postech.soat16.fase1.shared.test.infrastructure.PostgresTestResource;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@DisplayName("AuthService — Integration Tests")
class AuthServiceIT {

    private static final String RAW_PASSWORD = "S3nh@-F0rte";
    private static final String ROLE = "ADMIN";
    private static final long EXPIRATION_SECONDS = 8 * 3600L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    AuthService authService;

    @Inject
    AppUserRepository userRepository;

    private static String uniqueUsername(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private static JsonNode decodeClaims(String token) {
        try {
            byte[] payload = Base64.getUrlDecoder().decode(token.split("\\.")[1]);
            return OBJECT_MAPPER.readTree(payload);
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Falha ao decodificar o token JWT", exception);
        }
    }

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @RunOnVertxContext
        @DisplayName("persiste o hash bcrypt")
        void shouldPersistHashedPassword(UniAsserter asserter) {
            String username = uniqueUsername("persist");

            asserter.execute(() -> authService.createUser(username, RAW_PASSWORD, ROLE));
            asserter.assertThat(
                    () -> Panache.withSession(() -> userRepository.findByUsername(username)),
                    persisted -> {
                        assertNotNull(persisted);
                        assertEquals(username, persisted.getUsername());
                        assertEquals(ROLE, persisted.getRole());
                        assertTrue(persisted.isActive());
                        assertNotEquals(RAW_PASSWORD, persisted.getPassword());
                        assertTrue(BcryptUtil.matches(RAW_PASSWORD, persisted.getPassword()));
                    });
        }

        @Test
        @RunOnVertxContext
        @DisplayName("falha quando o username já existe")
        void shouldFailWhenUsernameAlreadyExists(UniAsserter asserter) {
            String username = uniqueUsername("dup");

            asserter.execute(() -> authService.createUser(username, RAW_PASSWORD, ROLE));
            asserter.assertFailedWith(
                    () -> authService.createUser(username, RAW_PASSWORD, ROLE),
                    IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @RunOnVertxContext
        @DisplayName("retorna token JWT para credenciais válidas")
        void shouldReturnTokenForValidCredentials(UniAsserter asserter) {
            String username = uniqueUsername("login-ok");

            asserter.execute(() -> authService.createUser(username, RAW_PASSWORD, ROLE));
            asserter.assertThat(
                    () -> authService.login(new LoginCommand(username, RAW_PASSWORD)),
                    response -> {
                        assertNotNull(response.token());
                        assertEquals(3, response.token().split("\\.").length);
                        assertEquals(username, response.username());
                        assertEquals(ROLE, response.role());
                        assertEquals(EXPIRATION_SECONDS, response.expiresIn());

                        JsonNode claims = decodeClaims(response.token());
                        assertEquals("oficina-api", claims.path("iss").asText());
                        assertEquals(username, claims.path("sub").asText());
                        assertEquals(ROLE, claims.path("groups").get(0).asText());
                        assertEquals(
                                EXPIRATION_SECONDS,
                                claims.path("exp").asLong() - claims.path("iat").asLong());
                    });
        }

        @Test
        @RunOnVertxContext
        @DisplayName("rejeita senha inválida")
        void shouldRejectInvalidPassword(UniAsserter asserter) {
            String username = uniqueUsername("login-bad");

            asserter.execute(() -> authService.createUser(username, RAW_PASSWORD, ROLE));
            asserter.assertFailedWith(
                    () -> authService.login(new LoginCommand(username, "senha-errada")),
                    InvalidCredentialsException.class);
        }

        @Test
        @RunOnVertxContext
        @DisplayName("rejeita usuário inexistente")
        void shouldRejectUnknownUser(UniAsserter asserter) {
            asserter.assertFailedWith(
                    () -> authService.login(
                            new LoginCommand(uniqueUsername("ghost"), RAW_PASSWORD)),
                    InvalidCredentialsException.class);
        }
    }
}
