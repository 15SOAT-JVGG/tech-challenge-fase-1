package br.com.fiap.postech.soat16.fase1.service;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.elytron.security.common.BcryptUtil;

@ApplicationScoped
public class PasswordService {

    public String hash(String rawPassword) {
        return BcryptUtil.bcryptHash(rawPassword);
    }

    public boolean matches(String rawPassword, String storedHash) {
        try {
            return BcryptUtil.matches(rawPassword, storedHash);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
