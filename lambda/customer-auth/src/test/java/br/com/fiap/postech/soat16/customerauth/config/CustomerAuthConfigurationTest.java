package br.com.fiap.postech.soat16.customerauth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.customerauth.support.TestKeys;

@DisplayName("CustomerAuthConfiguration — Testes unitários")
class CustomerAuthConfigurationTest {

    private static Map<String, String> requiredEnvironment() {
        Map<String, String> environment = new HashMap<>();
        environment.put("DB_HOST", "db.internal");
        environment.put("DB_NAME", "oficina");
        environment.put("DB_USERNAME", "oficina");
        environment.put("DB_PASSWORD", "s3cret");
        environment.put("JWT_PRIVATE_KEY_B64", TestKeys.privateKeyPemBase64());
        return environment;
    }

    @Test
    @DisplayName("deve aplicar os defaults de porta, TLS, issuer e validade do token")
    void shouldApplyDefaults() {
        CustomerAuthConfiguration configuration = CustomerAuthConfiguration.from(requiredEnvironment());

        assertEquals(5432, configuration.databasePort());
        assertEquals("require", configuration.databaseSslMode());
        assertEquals("oficina-api", configuration.jwtIssuer());
        assertEquals(Duration.ofMinutes(60), configuration.tokenLifetime());
        assertTrue(configuration.jwtPrivateKeyPem().startsWith("-----BEGIN PRIVATE KEY-----"));
        assertEquals(
                "jdbc:postgresql://db.internal:5432/oficina?sslmode=require&connectTimeout=5&socketTimeout=5"
                        + "&ApplicationName=customer-auth",
                configuration.jdbcUrl());
    }

    @Test
    @DisplayName("deve respeitar os valores informados no lugar dos defaults")
    void shouldHonorOverrides() {
        Map<String, String> environment = requiredEnvironment();
        environment.put("DB_PORT", "6543");
        environment.put("DB_SSLMODE", "disable");
        environment.put("JWT_ISSUER", "outro-issuer");
        environment.put("JWT_EXPIRATION_MINUTES", "15");

        CustomerAuthConfiguration configuration = CustomerAuthConfiguration.from(environment);

        assertEquals(6543, configuration.databasePort());
        assertEquals("disable", configuration.databaseSslMode());
        assertEquals("outro-issuer", configuration.jwtIssuer());
        assertEquals(Duration.ofMinutes(15), configuration.tokenLifetime());
    }

    @Test
    @DisplayName("deve listar todas as variáveis obrigatórias ausentes de uma vez")
    void shouldListMissingVariables() {
        Map<String, String> environment = requiredEnvironment();
        environment.remove("DB_PASSWORD");
        environment.put("JWT_PRIVATE_KEY_B64", " ");

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> CustomerAuthConfiguration.from(environment));

        assertTrue(exception.getMessage().contains("DB_PASSWORD"));
        assertTrue(exception.getMessage().contains("JWT_PRIVATE_KEY_B64"));
    }

    @Test
    @DisplayName("não deve expor senha nem chave privada ao virar texto")
    void shouldNotPrintSecrets() {
        String printed = CustomerAuthConfiguration.from(requiredEnvironment()).toString();

        assertFalse(printed.contains("s3cret"));
        assertFalse(printed.contains("PRIVATE KEY"));
    }

    @Test
    @DisplayName("deve montar o caso de uso com os adaptadores reais sem abrir conexão")
    void shouldBuildModuleWithoutConnecting() {
        assertNotNull(CustomerAuthModule.build(CustomerAuthConfiguration.from(requiredEnvironment())));
    }
}
