package br.com.fiap.postech.soat16.fase1.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import br.com.fiap.postech.soat16.fase1.model.WorkerProfile;

public record WorkerRequestDto(
    @NotBlank(message = "firstName cannot be blank")
    String firstName,
    @NotBlank(message = "lastName cannot be blank")
    String lastName,
    @NotBlank(message = "email cannot be blank")
    @Email(message = "email must be a valid email address")
    String email,
    String phoneNumber,
    @NotBlank(message = "password cannot be blank")
    @Size(min = 8, message = "password must have at least 8 characters")
    String password,
    @NotNull(message = "profile cannot be null")
    WorkerProfile profile
) { }
