package br.com.fiap.postech.soat16.fase1.controller;

import static io.restassured.RestAssured.given;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.security.PostgresTestResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Garante que as APIs administrativas exigem JWT (requisito do PDF) e que os endpoints públicos
 * (login e canal do cliente) permanecem acessíveis sem token.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@DisplayName("Security — proteção das APIs administrativas")
class SecurityIT {

    @Test
    @DisplayName("deve retornar 401 ao acessar uma API administrativa sem token")
    void shouldReturn401OnAdminEndpointWithoutToken() {
        given()
        .when()
            .get("/v1/work-orders")
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("deve liberar o login sem token")
    void shouldAllowLoginWithoutToken() {
        given()
            .contentType("application/json")
            .body("{\"username\":\"\",\"password\":\"\"}")
        .when()
            .post("/v1/auth/login")
        .then()
            .statusCode(400); // passa pela autorização (público); falha só na validação do corpo
    }

    @Test
    @DisplayName("deve liberar o canal público do cliente sem token")
    void shouldAllowPublicClientEndpointWithoutToken() {
        given()
        .when()
            .get("/v1/public/work-orders/" + UUID.randomUUID())
        .then()
            .statusCode(404); // público: passa pela autorização; OS inexistente → 404
    }
}
