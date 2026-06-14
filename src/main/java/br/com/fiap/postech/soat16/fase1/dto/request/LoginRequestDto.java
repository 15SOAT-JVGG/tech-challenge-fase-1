package br.com.fiap.postech.soat16.fase1.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
    @NotBlank(message = "Username é obrigatório") String username,
    @NotBlank(message = "Senha é obrigatória") String password
) {}
