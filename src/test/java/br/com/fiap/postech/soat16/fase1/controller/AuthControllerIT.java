package br.com.fiap.postech.soat16.fase1.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;

import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.security.AuthService;
import br.com.fiap.postech.soat16.fase1.security.PostgresTestResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;

/**
 * Testes de integração HTTP do endpoint POST /v1/auth/login contra um PostgreSQL real
 * (Testcontainers via {@link PostgresTestResource}). Exercita toda a stack:
 * RestAssured → AuthController → AuthService → AppUserRepository → Hibernate Reactive → PostgreSQL.
 * Cada teste usa um username único porque o schema é drop-and-create por execução (não por méthodo).
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@DisplayName("AuthController — Integration Tests (HTTP)")
class AuthControllerIT {

    private static final String LOGIN_PATH = "/v1/auth/login";
    private static final String RAW_PASSWORD = "S3nh@-F0rte";
    private static final String ROLE = "ADMIN";
    // Um JWT possui três segmentos base64url separados por ponto.
    private static final String JWT_PATTERN = "^[\\w-]+\\.[\\w-]+\\.[\\w-]+$";

    @Inject
    AuthService authService;

    private static String uniqueUsername(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    // Persiste o usuário fora do event loop, estabelecendo o contexto Vert.x exigido
    // pelo Hibernate Reactive (mesma abordagem do DataSeeder no startup).
    private void persistUser(String username) {
        try {
            VertxContextSupport.subscribeAndAwait(
                () -> authService.createUser(username, RAW_PASSWORD, ROLE));
        } catch (Throwable e) {
            throw new IllegalStateException("Falha ao preparar usuário de teste: " + username, e);
        }
    }

    private static String loginBody(String username, String password) {
        return "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password);
    }

    @Nested
    @DisplayName("POST /v1/auth/login")
    class Login {

        @Test
        @DisplayName("retorna 200 e token JWT para credenciais válidas")
        void shouldReturnTokenForValidCredentials() {
            String username = uniqueUsername("login-ok");
            persistUser(username);

            given()
                .contentType("application/json")
                .body(loginBody(username, RAW_PASSWORD))
            .when()
                .post(LOGIN_PATH)
            .then()
                .statusCode(200)
                .body("token", matchesPattern(JWT_PATTERN))
                .body("username", equalTo(username))
                .body("role", equalTo(ROLE))
                .body("expiresIn", is(greaterThan(0)));
        }

        @Test
        @DisplayName("retorna 401 quando a senha é inválida")
        void shouldReturn401ForInvalidPassword() {
            String username = uniqueUsername("login-bad");
            persistUser(username);

            given()
                .contentType("application/json")
                .body(loginBody(username, "senha-errada"))
            .when()
                .post(LOGIN_PATH)
            .then()
                .statusCode(401);
        }

        @Test
        @DisplayName("retorna 401 para usuário inexistente (sem vazar a inexistência)")
        void shouldReturn401ForUnknownUser() {
            given()
                .contentType("application/json")
                .body(loginBody(uniqueUsername("ghost"), RAW_PASSWORD))
            .when()
                .post(LOGIN_PATH)
            .then()
                .statusCode(401);
        }

        @Test
        @DisplayName("retorna 400 quando o corpo viola Bean Validation (campos em branco)")
        void shouldReturn400ForInvalidBody() {
            given()
                .contentType("application/json")
                .body(loginBody("", ""))
            .when()
                .post(LOGIN_PATH)
            .then()
                .statusCode(400)
                .body("error", notNullValue());
        }
    }
}
