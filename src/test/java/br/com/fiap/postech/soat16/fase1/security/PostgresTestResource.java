package br.com.fiap.postech.soat16.fase1.security;

import java.io.IOException;
import java.util.Map;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.PostgreSQLContainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Sobe um PostgreSQL real (Testcontainers) e injeta a URL reativa durante a execução.
 * DevServices não é usado porque a configuração base define quarkus.datasource.reactive.url,
 * e uma URL explícita desativa o recurso. Este recurso fornece o contêiner apenas para o teste,
 * sem alterar a configuração compartilhada.
 */
public class PostgresTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String SCHEMA = "oficina_mecanica";

    @SuppressWarnings("resource")
    private final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Override
    public Map<String, String> start() {
        postgres.start();
        createSchema();

        // Mantém o mesmo schema e search_path da produção; o Hibernate cria as tabelas nos testes.
        String reactiveUrl = "postgresql://%s:%d/%s?search_path=%s".formatted(
            postgres.getHost(),
            postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
            postgres.getDatabaseName(),
            SCHEMA);

        // O Flyway usa a conexão JDBC bloqueante. Sem esta sobrescrita, herdaria a URL base com
        // as credenciais do contêiner.
        String jdbcUrl = "jdbc:postgresql://%s:%d/%s?currentSchema=%s".formatted(
            postgres.getHost(),
            postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
            postgres.getDatabaseName(),
            SCHEMA);

        return Map.of(
            "quarkus.datasource.reactive.url", reactiveUrl,
            "quarkus.datasource.jdbc.url", jdbcUrl,
            "quarkus.datasource.username", postgres.getUsername(),
            "quarkus.datasource.password", postgres.getPassword()
        );
    }

    // Usa o psql porque o empacotamento de withInitScript diverge entre as versões dos módulos do
    // Testcontainers gerenciadas pelo Quarkus.
    private void createSchema() {
        try {
            Container.ExecResult result = postgres.execInContainer(
                "psql", "-U", postgres.getUsername(), "-d", postgres.getDatabaseName(),
                "-c", "CREATE SCHEMA IF NOT EXISTS \"%s\"".formatted(SCHEMA));
            if (result.getExitCode() != 0) {
                throw new IllegalStateException("Falha ao criar schema de teste: " + result.getStderr());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao criar schema de teste", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrompido ao criar schema de teste", e);
        }
    }

    @Override
    public void stop() {
        postgres.stop();
    }
}
