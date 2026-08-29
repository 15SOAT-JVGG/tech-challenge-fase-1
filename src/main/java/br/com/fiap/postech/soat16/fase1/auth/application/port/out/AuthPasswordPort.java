package br.com.fiap.postech.soat16.fase1.auth.application.port.out;

public interface AuthPasswordPort {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String storedHash);
}
