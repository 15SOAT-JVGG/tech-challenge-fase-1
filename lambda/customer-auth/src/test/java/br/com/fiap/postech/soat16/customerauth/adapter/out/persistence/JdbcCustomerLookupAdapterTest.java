package br.com.fiap.postech.soat16.customerauth.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.customerauth.domain.Cpf;

@DisplayName("JdbcCustomerLookupAdapter — Testes unitários")
class JdbcCustomerLookupAdapterTest {

    @Test
    @DisplayName("deve traduzir a falha de conexão em CustomerLookupException")
    void shouldWrapConnectionFailure() {
        var adapter = new JdbcCustomerLookupAdapter(() -> {
            throw new SQLException("connection refused");
        });

        CustomerLookupException exception =
                assertThrows(CustomerLookupException.class, () -> adapter.findByCpf(Cpf.of("52998224725")));

        assertSame(SQLException.class, exception.getCause().getClass());
    }

    @Test
    @DisplayName("deve traduzir a falha da consulta em CustomerLookupException")
    void shouldWrapQueryFailure() throws SQLException {
        Connection connection = mock(Connection.class);
        when(connection.isValid(anyInt())).thenReturn(true);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("relation does not exist"));
        var adapter = new JdbcCustomerLookupAdapter(() -> connection);

        assertThrows(CustomerLookupException.class, () -> adapter.findByCpf(Cpf.of("52998224725")));
    }
}
