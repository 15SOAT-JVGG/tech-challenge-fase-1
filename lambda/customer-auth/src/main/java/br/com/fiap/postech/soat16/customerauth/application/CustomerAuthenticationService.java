package br.com.fiap.postech.soat16.customerauth.application;

import br.com.fiap.postech.soat16.customerauth.application.port.out.CustomerLookupPort;
import br.com.fiap.postech.soat16.customerauth.application.port.out.CustomerTokenPort;
import br.com.fiap.postech.soat16.customerauth.domain.Cpf;
import br.com.fiap.postech.soat16.customerauth.domain.Customer;
import br.com.fiap.postech.soat16.customerauth.domain.exception.CustomerNotAuthorizedException;

public class CustomerAuthenticationService {

    private final CustomerLookupPort customers;
    private final CustomerTokenPort tokens;

    public CustomerAuthenticationService(CustomerLookupPort customers, CustomerTokenPort tokens) {
        this.customers = customers;
        this.tokens = tokens;
    }

    public IssuedToken authenticate(String rawCpf) {
        Cpf cpf = Cpf.of(rawCpf);
        Customer customer = customers.findByCpf(cpf)
                .orElseThrow(() -> new CustomerNotAuthorizedException("not-found"));
        customer.ensureCanAuthenticate();
        return tokens.issueFor(customer);
    }
}
