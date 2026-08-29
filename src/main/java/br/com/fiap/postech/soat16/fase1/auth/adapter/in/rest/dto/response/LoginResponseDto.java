package br.com.fiap.postech.soat16.fase1.auth.adapter.in.rest.dto.response;

public record LoginResponseDto(
        String token,
        String username,
        String role,
        long expiresIn
) {
}
