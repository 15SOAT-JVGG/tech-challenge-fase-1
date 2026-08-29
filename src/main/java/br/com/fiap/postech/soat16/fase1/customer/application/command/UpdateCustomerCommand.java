package br.com.fiap.postech.soat16.fase1.customer.application.command;

import java.util.UUID;

public record UpdateCustomerCommand(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber
) { }
