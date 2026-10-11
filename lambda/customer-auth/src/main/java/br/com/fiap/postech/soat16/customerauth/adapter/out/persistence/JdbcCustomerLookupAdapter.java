package br.com.fiap.postech.soat16.customerauth.adapter.out.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.postech.soat16.customerauth.application.port.out.CustomerLookupPort;
import br.com.fiap.postech.soat16.customerauth.domain.Cpf;
import br.com.fiap.postech.soat16.customerauth.domain.Customer;

/**
 * Lê a mesma tabela que a API grava. A conexão sobrevive entre invocações do mesmo ambiente de
 * execução — abrir TLS com o RDS a cada login dobraria a latência —, e não precisa de pool nem de
 * sincronização porque a Lambda entrega uma invocação por vez a cada ambiente.
 */
public class JdbcCustomerLookupAdapter implements CustomerLookupPort {

    private static final String FIND_BY_CPF = """
            SELECT customer_id, active
            FROM oficina_mecanica.customer
            WHERE document = ? AND document_type = 'CPF'
            """;
    private static final int VALIDATION_TIMEOUT_SECONDS = 2;

    private final ConnectionFactory connectionFactory;
    private Connection connection;

    public JdbcCustomerLookupAdapter(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Optional<Customer> findByCpf(Cpf cpf) {
        try (PreparedStatement query = openConnection().prepareStatement(FIND_BY_CPF)) {
            query.setString(1, cpf.digits());
            try (ResultSet result = query.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Customer(
                        result.getObject("customer_id", UUID.class),
                        result.getBoolean("active")));
            }
        } catch (SQLException exception) {
            throw new CustomerLookupException(exception);
        }
    }

    private Connection openConnection() throws SQLException {
        if (connection == null || !connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
            connection = connectionFactory.open();
        }
        return connection;
    }

    @FunctionalInterface
    public interface ConnectionFactory {

        Connection open() throws SQLException;
    }
}
