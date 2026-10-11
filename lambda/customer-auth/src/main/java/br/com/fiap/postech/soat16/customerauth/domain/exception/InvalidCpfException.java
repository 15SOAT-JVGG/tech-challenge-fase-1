package br.com.fiap.postech.soat16.customerauth.domain.exception;

public class InvalidCpfException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidCpfException() {
        super("CPF inválido");
    }
}
