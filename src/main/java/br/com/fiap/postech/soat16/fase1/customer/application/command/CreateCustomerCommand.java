package br.com.fiap.postech.soat16.fase1.customer.application.command;

public record CreateCustomerCommand(
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String document
) { }
