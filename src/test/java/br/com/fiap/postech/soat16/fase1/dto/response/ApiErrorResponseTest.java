package br.com.fiap.postech.soat16.fase1.dto.response;

import br.com.fiap.postech.soat16.fase1.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApiErrorResponse — Unit Tests")
class ApiErrorResponseTest {

    @Test
    @DisplayName("of() creates response with current timestamp")
    void ofCreatesWithTimestamp() {
        Instant before = Instant.now();

        ApiErrorResponse response = ApiErrorResponse.of(404, ErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "not found", "/v1/customer/123", null);

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
