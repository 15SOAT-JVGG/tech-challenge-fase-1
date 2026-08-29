package br.com.fiap.postech.soat16.fase1.worker.application.port.out;

public interface WorkerPasswordPort {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String storedHash);
}
