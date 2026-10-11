package br.com.fiap.postech.soat16.customerauth.adapter.out.persistence;

public class CustomerLookupException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CustomerLookupException(Throwable cause) {
        super("Falha ao consultar o cliente no banco", cause);
    }
}
