package br.com.fiap.postech.soat16.fase1.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.dto.request.LoginRequestDto;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

/**
 * Testes de integração contra um PostgreSQL real (DevServices/Testcontainers).
 * Executam toda a stack reativa: AuthService → AppUserRepository → Hibernate Reactive → PostgreSQL.
 * Foco principal: confirmar que o offload do bcrypt para o worker pool
 * (runSubscriptionOn / emitOn) preserva a sessão reativa via context propagation
 * do Quarkus — caso contrário o persist/read falharia com "No current Mutiny.Session".
 * Roda apenas no profile itest (mvn test -Pitest) por exigir Docker.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@DisplayName("AuthService — Integration Tests")
class AuthServiceIT {

    private static final String RAW_PASSWORD = "S3nh@-F0rte";
    private static final String ROLE = "ADMIN";

    @Inject
    AuthService authService;

    @Inject
    AppUserRepository userRepository;

    private static String uniqueUsername(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @RunOnVertxContext
        @DisplayName("persiste o hash bcrypt mesmo após o offload para o worker pool")
        void shouldPersistHashedPasswordAcrossWorkerPoolBoundary(UniAsserter asserter) {
            String username = uniqueUsername("persist");

            asserter.execute(() -> authService.createUser(username, RAW_PASSWORD, ROLE));

            asserter.assertThat(
                () -> Panache.withSession(() -> userRepository.findByUsername(username)),
                persisted -> {
                    assertNotNull(persisted, "usuário deveria ter sido persistido após o offload do bcrypt");
                    assertEquals(username, persisted.getUsername());
                    assertEquals(ROLE, persisted.getRole());
                    assertTrue(persisted.isActive());
                    // senha nunca persistida em texto plano e validável pelo bcrypt
                    assertNotEquals(RAW_PASSWORD, persisted.getPassword());
                    assertTrue(BcryptUtil.matches(RAW_PASSWORD, persisted.getPassword()),
                        "hash persistido deve corresponder à senha original");
                });
        }

        @Test
        @RunOnVertxContext
        @DisplayName("falha com IllegalArgumentException quando o username já existe")
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
                () -> authService.login(new LoginRequestDto(username, RAW_PASSWORD)),
                response -> {
                    assertNotNull(response.token());
                    assertEquals(3, response.token().split("\\.").length, "token deve ter formato JWT");
                    assertEquals(username, response.username());
                    assertEquals(ROLE, response.role());
                    assertTrue(response.expiresIn() > 0);
                });
        }

        @Test
        @RunOnVertxContext
        @DisplayName("rejeita senha inválida com NotAuthorizedException")
        void shouldRejectInvalidPassword(UniAsserter asserter) {
            String username = uniqueUsername("login-bad");

            asserter.execute(() -> authService.createUser(username, RAW_PASSWORD, ROLE));
            asserter.assertFailedWith(
                () -> authService.login(new LoginRequestDto(username, "senha-errada")),
                NotAuthorizedException.class);
        }

        @Test
        @RunOnVertxContext
        @DisplayName("rejeita usuário inexistente com NotAuthorizedException (comparação em tempo constante)")
        void shouldRejectUnknownUser(UniAsserter asserter) {
            asserter.assertFailedWith(
                () -> authService.login(new LoginRequestDto(uniqueUsername("ghost"), RAW_PASSWORD)),
                NotAuthorizedException.class);
        }
    }
}
