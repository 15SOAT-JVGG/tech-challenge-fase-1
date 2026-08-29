package br.com.fiap.postech.soat16.fase1.worker.adapter.out.security;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.shared.infrastructure.security.PasswordService;
import br.com.fiap.postech.soat16.fase1.worker.application.port.out.WorkerPasswordPort;

import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class WorkerPasswordAdapter implements WorkerPasswordPort {

    private final PasswordService passwordService;

    @Override
    public String hash(String rawPassword) {
        return passwordService.hash(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String storedHash) {
        return passwordService.matches(rawPassword, storedHash);
    }
}
