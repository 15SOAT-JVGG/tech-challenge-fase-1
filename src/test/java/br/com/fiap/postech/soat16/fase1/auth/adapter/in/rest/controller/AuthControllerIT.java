package br.com.fiap.postech.soat16.fase1.auth.adapter.in.rest.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;

import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.auth.application.AuthService;
import br.com.fiap.postech.soat16.fase1.shared.test.infrastructure.JwtKeyPairTestResource;
import br.com.fiap.postech.soat16.fase1.shared.test.infrastructure.PostgresTestResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@QuarkusTestResource(JwtKeyPairTestResource.class)
@DisplayName("AuthController — Integration Tests (HTTP)")
class AuthControllerIT {

    private static final String LOGIN_PATH = "/v1/auth/login";
    private static final String RAW_PASSWORD = "S3nh@-F0rte";
    private static final String ROLE = "ADMIN";
    private static final String JWT_PATTERN = "^[\\w-]+\\.[\\w-]+\\.[\\w-]+$";

    @Inject
    AuthService authService;

    private static String uniqueUsername(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private void persistUser(String username) {
        try {
            VertxContextSupport.subscribeAndAwait(
                    () -> authService.createUser(username, RAW_PASSWORD, ROLE));
        } catch (Throwable exception) {
            throw new IllegalStateException(
                    "Falha ao preparar usuário de teste: " + username,
                    exception);
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
                .body("expiresIn", equalTo(8 * 3600));
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
        @DisplayName("retorna 401 para usuário inexistente")
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
        @DisplayName("retorna 400 para campos em branco")
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
