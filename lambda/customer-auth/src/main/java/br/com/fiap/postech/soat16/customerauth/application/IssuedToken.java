package br.com.fiap.postech.soat16.customerauth.application;

import java.util.UUID;

public record IssuedToken(String token, UUID customerId, String role, long expiresIn) {
}
