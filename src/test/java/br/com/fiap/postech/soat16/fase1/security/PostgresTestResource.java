package br.com.fiap.postech.soat16.fase1.security;

import java.io.IOException;
import java.util.Map;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.PostgreSQLContainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Sobe um PostgreSQL real (Testcontainers) e injeta a URL reativa em runtime.
 * Motivo de não usar DevServices: a config base define quarkus.datasource.reactive.url
 * (necessária ao profile de runtime "default"), e uma URL explícita suprime o DevServices.
 * Este resource fornece o container e sobrescreve a URL apenas para o teste, sem tocar
 * na configuração compartilhada.
 */
public class PostgresTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String SCHEMA = "oficina_mecanica";

    @SuppressWarnings("resource")
    private final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Override
    public Map<String, String> start() {
        postgres.start();
        createSchema();

        // Paridade com produção: mesmo schema e search_path; o Hibernate (%test: drop-and-create) cria as tabelas.
        String reactiveUrl = "postgresql://%s:%d/%s?search_path=%s".formatted(
            postgres.getHost(),
            postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
            postgres.getDatabaseName(),
            SCHEMA);

        // O Flyway usa o datasource JDBC (blocking), não o reativo. Sem este override ele
        // herda quarkus.datasource.jdbc.url da config base (ex.: INFRA_HOST_POSTGRES local),
        // mas com as credenciais do container — causando "password authentication failed".
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

    // Cria o schema via psql no socket local (trust) — evita o withInitScript, cujo layout shaded
    // diverge entre o módulo postgresql (1.20.x) e o testcontainers-core (gerido pelo quarkus-bom).
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
