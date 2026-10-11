package br.com.fiap.postech.soat16.customerauth.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import br.com.fiap.postech.soat16.customerauth.domain.Cpf;
import br.com.fiap.postech.soat16.customerauth.domain.Customer;

/**
 * A tabela é criada com as colunas que a API mapeia em {@code CustomerJpaEntity} e que a
 * migration V3 completa com {@code active}. É esse o contrato entre a função e o banco da API.
 */
@DisplayName("JdbcCustomerLookupAdapter — Testes de integração")
class JdbcCustomerLookupAdapterIT {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    private static final UUID ACTIVE_ID = UUID.randomUUID();
    private static final UUID INACTIVE_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();

    @BeforeAll
    static void createSchema() throws SQLException {
        POSTGRES.start();
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA oficina_mecanica");
            statement.execute("""
                    CREATE TABLE oficina_mecanica.customer (
                        customer_id UUID PRIMARY KEY,
                        first_name VARCHAR(255) NOT NULL,
                        phone_number VARCHAR(255) NOT NULL,
                        document VARCHAR(255) NOT NULL UNIQUE,
                        document_type VARCHAR(255) NOT NULL,
                        active BOOLEAN NOT NULL DEFAULT TRUE
                    )
                    """);
        }
        insert(ACTIVE_ID, "52998224725", "CPF", true);
        insert(INACTIVE_ID, "12345678305", "CPF", false);
        insert(COMPANY_ID, "11222333000181", "CNPJ", true);
    }

    @AfterAll
    static void stopPostgres() {
        POSTGRES.stop();
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static void insert(UUID id, String document, String type, boolean active) throws SQLException {
        try (Connection connection = connection();
                PreparedStatement insert = connection.prepareStatement("""
                        INSERT INTO oficina_mecanica.customer
                            (customer_id, first_name, phone_number, document, document_type, active)
                        VALUES (?, 'Cliente', '+5511999999999', ?, ?, ?)
                        """)) {
            insert.setObject(1, id);
            insert.setString(2, document);
            insert.setString(3, type);
            insert.setBoolean(4, active);
            insert.executeUpdate();
        }
    }

    @Nested
    @DisplayName("findByCpf")
    class FindByCpf {

        private final JdbcCustomerLookupAdapter adapter =
                new JdbcCustomerLookupAdapter(JdbcCustomerLookupAdapterIT::connection);

        @Test
        @DisplayName("deve encontrar o cliente ativo pelo CPF normalizado")
        void shouldFindActiveCustomer() {
            Optional<Customer> found = adapter.findByCpf(Cpf.of("529.982.247-25"));

            assertEquals(Optional.of(new Customer(ACTIVE_ID, true)), found);
        }

        @Test
        @DisplayName("deve devolver o cliente inativo com o status, para o domínio decidir")
        void shouldFindInactiveCustomer() {
            assertEquals(Optional.of(new Customer(INACTIVE_ID, false)), adapter.findByCpf(Cpf.of("12345678305")));
        }

        @Test
        @DisplayName("deve devolver vazio para CPF sem cadastro")
        void shouldReturnEmptyForUnknownCpf() {
            assertTrue(adapter.findByCpf(Cpf.of("11144477735")).isEmpty());
        }

        @Test
        @DisplayName("deve reaproveitar a conexão entre consultas e reabri-la quando cair")
        void shouldReuseAndReopenConnection() throws SQLException {
            AtomicInteger opened = new AtomicInteger();
            Connection[] last = new Connection[1];
            var reusing = new JdbcCustomerLookupAdapter(() -> {
                opened.incrementAndGet();
                last[0] = connection();
                return last[0];
            });

            reusing.findByCpf(Cpf.of("52998224725"));
            reusing.findByCpf(Cpf.of("52998224725"));
            assertEquals(1, opened.get());

            last[0].close();
            assertFalse(reusing.findByCpf(Cpf.of("52998224725")).isEmpty());
            assertEquals(2, opened.get());
        }
    }
}
