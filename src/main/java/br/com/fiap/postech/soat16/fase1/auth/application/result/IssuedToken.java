package br.com.fiap.postech.soat16.fase1.auth.application.result;

public record IssuedToken(
        String token,
        long expiresIn
) {
}
