package br.com.fiap.postech.soat16.fase1.auth.adapter.in.rest.dto.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LoginRequestDto — Unit Tests")
class LoginRequestDtoTest {

    @Test
    @DisplayName("should expose username and password")
    void shouldExposeUsernameAndPassword() {
        LoginRequestDto dto = new LoginRequestDto("admin", "secret");

        assertEquals("admin", dto.username());
        assertEquals("secret", dto.password());
    }
}
