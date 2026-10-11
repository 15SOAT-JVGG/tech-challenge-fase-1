package br.com.fiap.postech.soat16.customerauth.adapter.in.apigateway;

import java.util.UUID;

/**
 * Mesmos campos {@code token}, {@code role} e {@code expiresIn} do login de funcionário da API.
 */
public record CustomerLoginResponse(String token, UUID customerId, String role, long expiresIn) {
}
