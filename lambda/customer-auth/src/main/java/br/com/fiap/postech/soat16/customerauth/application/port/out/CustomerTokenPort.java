package br.com.fiap.postech.soat16.customerauth.application.port.out;

import br.com.fiap.postech.soat16.customerauth.application.IssuedToken;
import br.com.fiap.postech.soat16.customerauth.domain.Customer;

public interface CustomerTokenPort {

    IssuedToken issueFor(Customer customer);
}
