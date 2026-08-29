package br.com.fiap.postech.soat16.fase1.auth.adapter.out.security;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.auth.application.port.out.AuthPasswordPort;
import br.com.fiap.postech.soat16.fase1.shared.infrastructure.security.PasswordService;

import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class AuthPasswordAdapter implements AuthPasswordPort {

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
