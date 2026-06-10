package br.com.fiap.postech.soat16.fase1.service;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.elytron.security.common.BcryptUtil;

@ApplicationScoped
public class PasswordService {

    private static final String BCRYPT_PREFIX = "$2";

    public String hash(String rawPassword) {
        return BcryptUtil.bcryptHash(rawPassword);
    }

    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || !storedHash.startsWith(BCRYPT_PREFIX)) {
            return false;
        }
        return BcryptUtil.matches(rawPassword, storedHash);
    }
}
