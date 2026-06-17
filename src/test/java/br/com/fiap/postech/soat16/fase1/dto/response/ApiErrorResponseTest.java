package br.com.fiap.postech.soat16.fase1.dto.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.dto.response.error.ApiErrorResponseDto;
import br.com.fiap.postech.soat16.fase1.exception.ErrorType;

@DisplayName("ApiErrorResponse — Unit Tests")
class ApiErrorResponseTest {

    @Test
    @DisplayName("of() creates response with current timestamp")
    void ofCreatesWithTimestamp() {
        Instant before = Instant.now();

        ApiErrorResponseDto response = ApiErrorResponseDto.of(
                404,
                ErrorType.NOT_FOUND,
                "CUSTOMER_NOT_FOUND",
                "not found",
                "/v1/customer/123",
                null);

        assertNotNull(response);
        assertEquals(404, response.status());
        assertEquals(ErrorType.NOT_FOUND, response.type());
        assertEquals("CUSTOMER_NOT_FOUND", response.code());
        assertEquals("not found", response.message());
        assertEquals("/v1/customer/123", response.path());
        assertNull(response.traceId());
        assertFalse(response.timestamp().isBefore(before));
    }
}
