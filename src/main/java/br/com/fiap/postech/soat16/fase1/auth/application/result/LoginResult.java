package br.com.fiap.postech.soat16.fase1.auth.application.result;

public record LoginResult(
        String token,
        String username,
        String role,
        long expiresIn
) {
}
