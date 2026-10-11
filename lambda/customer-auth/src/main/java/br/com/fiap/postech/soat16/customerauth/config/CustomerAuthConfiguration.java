package br.com.fiap.postech.soat16.customerauth.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tudo chega por variável de ambiente da função, preenchida pelo Terraform. A chave privada vem em
 * base64 — o mesmo valor do secret {@code JWT_PRIVATE_KEY_B64} que a pipeline da API usa — porque
 * quebras de linha de um PEM não sobrevivem bem a variáveis de ambiente.
 */
public record CustomerAuthConfiguration(
        String databaseHost,
        int databasePort,
        String databaseName,
        String databaseUsername,
        String databasePassword,
        String databaseSslMode,
        String jwtPrivateKeyPem,
        String jwtIssuer,
        Duration tokenLifetime
) {

    private static final List<String> REQUIRED = List.of(
            "DB_HOST",
            "DB_NAME",
            "DB_USERNAME",
            "DB_PASSWORD",
            "JWT_PRIVATE_KEY_B64");
    private static final int CONNECT_TIMEOUT_SECONDS = 5;

    public static CustomerAuthConfiguration from(Map<String, String> environment) {
        List<String> missing = REQUIRED.stream()
                .filter(name -> isBlank(environment.get(name)))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Variáveis de ambiente obrigatórias ausentes: " + String.join(", ", missing));
        }
        return new CustomerAuthConfiguration(
                environment.get("DB_HOST"),
                Integer.parseInt(valueOrDefault(environment, "DB_PORT", "5432")),
                environment.get("DB_NAME"),
                environment.get("DB_USERNAME"),
                environment.get("DB_PASSWORD"),
                valueOrDefault(environment, "DB_SSLMODE", "require"),
                new String(Base64.getDecoder().decode(environment.get("JWT_PRIVATE_KEY_B64").trim()),
                        StandardCharsets.US_ASCII),
                valueOrDefault(environment, "JWT_ISSUER", "oficina-api"),
                Duration.ofMinutes(Long.parseLong(valueOrDefault(environment, "JWT_EXPIRATION_MINUTES", "60"))));
    }

    /**
     * Os timeouts curtos fazem um banco inalcançável virar 500 em segundos, dentro do timeout da
     * função, em vez de uma invocação pendurada até a Lambda matá-la.
     */
    public String jdbcUrl() {
        return "jdbc:postgresql://%s:%d/%s?sslmode=%s&connectTimeout=%d&socketTimeout=%d&ApplicationName=customer-auth"
                .formatted(databaseHost, databasePort, databaseName, databaseSslMode,
                        CONNECT_TIMEOUT_SECONDS, CONNECT_TIMEOUT_SECONDS);
    }

    @Override
    public String toString() {
        return "CustomerAuthConfiguration[databaseHost=%s, databaseName=%s, jwtIssuer=%s, tokenLifetime=%s]"
                .formatted(databaseHost, databaseName, jwtIssuer, tokenLifetime);
    }

    private static String valueOrDefault(Map<String, String> environment, String name, String fallback) {
        return Optional.ofNullable(environment.get(name)).filter(value -> !value.isBlank()).orElse(fallback);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
