package br.com.fiap.postech.soat16.fase1.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PasswordService - Unit Tests")
class PasswordServiceTest {

    private final PasswordService service = new PasswordService();

    @Test
    @DisplayName("should hash and validate password")
    void shouldHashAndValidatePassword() {
        String hash = service.hash("strong-password");

        assertNotEquals("strong-password", hash);
        assertTrue(service.matches("strong-password", hash));
        assertFalse(service.matches("wrong-password", hash));
    }

    @Test
    @DisplayName("should reject invalid stored hash")
    void shouldRejectInvalidStoredHash() {
        assertFalse(service.matches("strong-password", "invalid"));
    }
}
