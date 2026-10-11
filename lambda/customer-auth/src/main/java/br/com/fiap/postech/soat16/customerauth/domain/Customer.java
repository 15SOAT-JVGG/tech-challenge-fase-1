package br.com.fiap.postech.soat16.customerauth.domain;

import java.util.UUID;

import br.com.fiap.postech.soat16.customerauth.domain.exception.CustomerNotAuthorizedException;

/**
 * O recorte do cliente que a autenticação precisa: quem é e se pode entrar.
 */
public record Customer(UUID id, boolean active) {

    public void ensureCanAuthenticate() {
        if (!active) {
            throw new CustomerNotAuthorizedException("inactive");
        }
    }
}
