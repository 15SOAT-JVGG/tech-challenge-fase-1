package br.com.fiap.postech.soat16.fase1.auth.adapter.in.rest;

import static io.restassured.RestAssured.given;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.shared.test.infrastructure.JwtKeyPairTestResource;
import br.com.fiap.postech.soat16.fase1.shared.test.infrastructure.PostgresTestResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.build.Jwt;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@QuarkusTestResource(JwtKeyPairTestResource.class)
@DisplayName("Security — proteção das APIs administrativas")
class SecurityIT {

    private static String tokenFor(String role) {
        return Jwt.issuer("oficina-api")
                .upn("integration-test-" + role.toLowerCase(Locale.ROOT))
                .groups(Set.of(role))
                .expiresIn(Duration.ofHours(1))
                .sign();
    }

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
    @DisplayName("deve liberar a fila operacional ao MECHANIC")
    void shouldAllowOperationalQueueToMechanic() {
        given()
            .header("Authorization", "Bearer " + tokenFor("MECHANIC"))
        .when()
            .get("/v1/work-orders")
        .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("deve retornar 403 ao pedir a métrica de tempo médio como MECHANIC")
    void shouldReturn403OnAdminOnlyMetricAsMechanic() {
        given()
            .header("Authorization", "Bearer " + tokenFor("MECHANIC"))
        .when()
            .get("/v1/work-orders/metrics/average-execution-time")
        .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("deve liberar a métrica de tempo médio ao ADMIN")
    void shouldAllowAdminOnlyMetricToAdmin() {
        given()
            .header("Authorization", "Bearer " + tokenFor("ADMIN"))
        .when()
            .get("/v1/work-orders/metrics/average-execution-time")
        .then()
            .statusCode(200);
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
            .statusCode(400);
    }

    // O 400 é a resposta do recurso ao link forjado: chegar até ela é a prova de que o canal do
    // cliente não exige token de autenticação.
    @Test
    @DisplayName("deve liberar o canal público do cliente sem token")
    void shouldAllowPublicClientEndpointWithoutToken() {
        given()
        .when()
            .get("/v1/public/work-orders/tracking/nao-e-um-link-valido")
        .then()
            .statusCode(400);
    }
}
