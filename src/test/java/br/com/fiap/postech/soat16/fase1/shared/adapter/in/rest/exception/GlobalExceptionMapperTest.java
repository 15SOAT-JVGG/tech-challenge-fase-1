package br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.Map;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.shared.domain.exception.BusinessException;
import br.com.fiap.postech.soat16.fase1.shared.domain.exception.ResourceNotFoundException;

@DisplayName("GlobalExceptionMapper — Unit Tests")
class GlobalExceptionMapperTest {

    private GlobalExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new GlobalExceptionMapper();
    }

    @Nested
    @DisplayName("known exceptions")
    class KnownExceptions {

        @Test
        @DisplayName("maps missing resources to HTTP 404")
        void mapsTo404() {
            Response response = mapper.toResponse(
                    new ResourceNotFoundException("Vehicle", 42L));

            assertEquals(404, response.getStatus());
            Map<String, Object> body = responseBody(response);
            assertEquals("Not Found", body.get("error"));
            assertEquals("Vehicle not found with id: 42", body.get("message"));
            assertNotNull(body.get("timestamp"));
        }

        @Test
        @DisplayName("maps business failures to HTTP 422")
        void mapsTo422() {
            Response response = mapper.toResponse(
                    new BusinessException("Insufficient stock"));

            assertEquals(422, response.getStatus());
            Map<String, Object> body = responseBody(response);
            assertEquals("Unprocessable Entity", body.get("error"));
            assertEquals("Insufficient stock", body.get("message"));
        }

        @Test
        @DisplayName("maps illegal arguments to HTTP 400")
        void mapsTo400() {
            Response response = mapper.toResponse(
                    new IllegalArgumentException("User already exists: admin"));

            assertEquals(400, response.getStatus());
            assertEquals(
                    "Bad Request",
                    responseBody(response).get("error"));
        }

        @Test
        @DisplayName("keeps a WebApplicationException response")
        void keepsNativeStatus() {
            Response response = mapper.toResponse(
                    new NotAuthorizedException("Invalid credentials", "Bearer"));

            assertEquals(401, response.getStatus());
        }
    }

    @Nested
    @DisplayName("unexpected exceptions")
    class Unexpected {

        @Test
        @DisplayName("maps to HTTP 500 without leaking details")
        void mapsTo500WithCorrelationId() {
            Response response = mapper.toResponse(
                    new NullPointerException("internal secret detail"));

            assertEquals(500, response.getStatus());
            Map<String, Object> body = responseBody(response);
            assertEquals("Internal Server Error", body.get("error"));
            assertEquals("Internal server error", body.get("message"));
            assertNotNull(body.get("correlationId"));
            assertNotNull(body.get("timestamp"));
        }

        @Test
        @DisplayName("generates a unique correlation id")
        void generatesUniqueCorrelationIds() {
            Map<String, Object> first = responseBody(
                    mapper.toResponse(new RuntimeException("boom")));
            Map<String, Object> second = responseBody(
                    mapper.toResponse(new RuntimeException("boom")));

            assertNotSame(first.get("correlationId"), second.get("correlationId"));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> responseBody(Response response) {
        return (Map<String, Object>) response.getEntity();
    }
}
