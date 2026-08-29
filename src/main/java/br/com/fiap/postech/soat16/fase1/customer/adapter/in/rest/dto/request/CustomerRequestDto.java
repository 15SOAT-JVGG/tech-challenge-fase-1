package br.com.fiap.postech.soat16.fase1.customer.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerRequestDto(
        @NotBlank(message = "firstName cannot be blank")
        String firstName,
        @NotBlank(message = "lastName cannot be blank")
        String lastName,
        @NotBlank(message = "email cannot be blank")
        @Email(message = "email must be a valid email address")
        String email,
        @NotBlank(message = "phoneNumber cannot be blank")
        String phoneNumber,
        @NotBlank(message = "document cannot be blank")
        String document
) { }
