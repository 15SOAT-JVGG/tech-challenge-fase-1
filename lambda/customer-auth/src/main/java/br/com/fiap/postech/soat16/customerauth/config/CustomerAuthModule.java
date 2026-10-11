package br.com.fiap.postech.soat16.customerauth.config;

import java.sql.DriverManager;
import java.time.Clock;

import br.com.fiap.postech.soat16.customerauth.adapter.out.persistence.JdbcCustomerLookupAdapter;
import br.com.fiap.postech.soat16.customerauth.adapter.out.security.PemPrivateKeys;
import br.com.fiap.postech.soat16.customerauth.adapter.out.security.RsaJwtCustomerTokenAdapter;
import br.com.fiap.postech.soat16.customerauth.application.CustomerAuthenticationService;

/**
 * Monta o caso de uso com os adaptadores reais. A chave é lida aqui, na inicialização: uma chave
 * ausente ou corrompida derruba o cold start com a causa no log, em vez de cada login responder 500.
 */
public final class CustomerAuthModule {

    private CustomerAuthModule() {
    }

    public static CustomerAuthenticationService build(CustomerAuthConfiguration configuration) {
        var lookup = new JdbcCustomerLookupAdapter(() -> DriverManager.getConnection(
                configuration.jdbcUrl(), configuration.databaseUsername(), configuration.databasePassword()));
        var tokens = new RsaJwtCustomerTokenAdapter(
                PemPrivateKeys.parse(configuration.jwtPrivateKeyPem()),
                configuration.jwtIssuer(),
                configuration.tokenLifetime(),
                Clock.systemUTC());
        return new CustomerAuthenticationService(lookup, tokens);
    }
}
