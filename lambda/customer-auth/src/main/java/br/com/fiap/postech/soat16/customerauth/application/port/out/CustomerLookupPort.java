package br.com.fiap.postech.soat16.customerauth.application.port.out;

import java.util.Optional;

import br.com.fiap.postech.soat16.customerauth.domain.Cpf;
import br.com.fiap.postech.soat16.customerauth.domain.Customer;

public interface CustomerLookupPort {

    Optional<Customer> findByCpf(Cpf cpf);
}
