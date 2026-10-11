package br.com.fiap.postech.soat16.customerauth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.postech.soat16.customerauth.application.port.out.CustomerLookupPort;
import br.com.fiap.postech.soat16.customerauth.application.port.out.CustomerTokenPort;
import br.com.fiap.postech.soat16.customerauth.domain.Cpf;
import br.com.fiap.postech.soat16.customerauth.domain.Customer;
import br.com.fiap.postech.soat16.customerauth.domain.exception.CustomerNotAuthorizedException;
import br.com.fiap.postech.soat16.customerauth.domain.exception.InvalidCpfException;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerAuthenticationService — Testes unitários")
class CustomerAuthenticationServiceTest {

    private static final String CPF = "529.982.247-25";
    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @Mock
    private CustomerLookupPort customers;

    @Mock
    private CustomerTokenPort tokens;

    private CustomerAuthenticationService service;

    @BeforeEach
    void setUp() {
        service = new CustomerAuthenticationService(customers, tokens);
    }

    @Test
    @DisplayName("deve emitir o token do cliente ativo encontrado pelo CPF normalizado")
    void shouldIssueTokenForActiveCustomer() {
        Customer customer = new Customer(CUSTOMER_ID, true);
        IssuedToken issued = new IssuedToken("jwt", CUSTOMER_ID, "CUSTOMER", 3600);
        when(customers.findByCpf(Cpf.of("52998224725"))).thenReturn(Optional.of(customer));
        when(tokens.issueFor(customer)).thenReturn(issued);

        assertSame(issued, service.authenticate(CPF));
    }

    @Test
    @DisplayName("deve recusar o CPF inválido sem consultar o banco")
    void shouldRejectInvalidCpfWithoutLookup() {
        assertThrows(InvalidCpfException.class, () -> service.authenticate("123"));

        verifyNoInteractions(customers, tokens);
    }

    @Test
    @DisplayName("deve recusar o CPF que não pertence a cliente algum")
    void shouldRejectUnknownCustomer() {
        when(customers.findByCpf(any())).thenReturn(Optional.empty());

        CustomerNotAuthorizedException exception =
                assertThrows(CustomerNotAuthorizedException.class, () -> service.authenticate(CPF));

        assertEquals("not-found", exception.reason());
        verifyNoInteractions(tokens);
    }

    @Test
    @DisplayName("deve recusar o cliente inativo com a mesma exceção do inexistente")
    void shouldRejectInactiveCustomer() {
        when(customers.findByCpf(any())).thenReturn(Optional.of(new Customer(CUSTOMER_ID, false)));

        CustomerNotAuthorizedException exception =
                assertThrows(CustomerNotAuthorizedException.class, () -> service.authenticate(CPF));

        assertEquals("inactive", exception.reason());
        assertEquals("CPF não autorizado", exception.getMessage());
        verify(tokens, never()).issueFor(any());
    }
}
