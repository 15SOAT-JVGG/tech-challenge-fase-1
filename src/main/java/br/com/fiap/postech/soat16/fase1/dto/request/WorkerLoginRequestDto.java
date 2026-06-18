package br.com.fiap.postech.soat16.fase1.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record WorkerLoginRequestDto(
    @NotBlank(message = "email cannot be blank")
    @Email(message = "email must be a valid email address")
    String email,
    @NotBlank(message = "password cannot be blank")
    String password
) { }
