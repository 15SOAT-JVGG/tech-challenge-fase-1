package br.com.fiap.postech.soat16.customerauth.domain.exception;

/**
 * Cliente inexistente e cliente inativo recebem a mesma resposta: distinguir os dois diria a quem
 * testa CPFs quais deles pertencem a clientes da oficina. O motivo fica só no log.
 */
public class CustomerNotAuthorizedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String reason;

    public CustomerNotAuthorizedException(String reason) {
        super("CPF não autorizado");
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
