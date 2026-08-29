package br.com.fiap.postech.soat16.fase1.shared.domain.exception;

public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
