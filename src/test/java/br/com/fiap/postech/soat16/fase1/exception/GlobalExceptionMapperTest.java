package br.com.fiap.postech.soat16.fase1.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GlobalExceptionMapper (catch-all) — Unit Tests")
class GlobalExceptionMapperTest {

    private GlobalExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new GlobalExceptionMapper();
    }

    @Nested
    @DisplayName("ResourceNotFoundException")
    class NotFound {

        @Test
        @DisplayName("maps to HTTP 404 with the exception message")
        void mapsTo404() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Vehicle", 42L);

            Response response = mapper.toResponse(ex);

            assertEquals(404, response.getStatus());
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getEntity();
            assertEquals(404, body.get("status"));
            assertEquals("Not Found", body.get("error"));
            assertEquals("Vehicle not found with id: 42", body.get("message"));
            assertNotNull(body.get("timestamp"));
        }
    }

    @Nested
    @DisplayName("BusinessException")
    class Business {

        @Test
        @DisplayName("maps to HTTP 422 (Unprocessable Entity)")
        void mapsTo422() {
            BusinessException ex = new BusinessException("Insufficient stock");

            Response response = mapper.toResponse(ex);

            assertEquals(422, response.getStatus());
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getEntity();
            assertEquals("Unprocessable Entity", body.get("error"));
            assertEquals("Insufficient stock", body.get("message"));
        }
    }

    @Nested
    @DisplayName("IllegalArgumentException")
    class IllegalArgument {

        @Test
        @DisplayName("maps to HTTP 400 (Bad Request)")
        void mapsTo400() {
            IllegalArgumentException ex = new IllegalArgumentException("User already exists: admin");

            Response response = mapper.toResponse(ex);

            assertEquals(400, response.getStatus());
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getEntity();
            assertEquals("Bad Request", body.get("error"));
            assertEquals("User already exists: admin", body.get("message"));
        }
    }

    @Nested
    @DisplayName("WebApplicationException")
    class WebApplication {

        @Test
        @DisplayName("keeps the native response status instead of overriding it")
        void keepsNativeStatus() {
            NotAuthorizedException ex = new NotAuthorizedException("Invalid credentials", "Bearer");

            Response response = mapper.toResponse(ex);

            assertEquals(401, response.getStatus());
        }
    }

    @Nested
    @DisplayName("unexpected exceptions")
    class Unexpected {

        @Test
        @DisplayName("maps to HTTP 500 without leaking the original message, with a correlation id")
        void mapsTo500WithCorrelationId() {
            NullPointerException ex = new NullPointerException("internal secret detail");

            Response response = mapper.toResponse(ex);

            assertEquals(500, response.getStatus());
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getEntity();
            assertEquals(500, body.get("status"));
            assertEquals("Internal Server Error", body.get("error"));
            assertEquals("Internal server error", body.get("message"));
            assertNotNull(body.get("correlationId"));
            assertNotNull(body.get("timestamp"));
        }

        @Test
        @DisplayName("generates a different correlation id for each occurrence")
        void generatesUniqueCorrelationIds() {
            Response first = mapper.toResponse(new RuntimeException("boom"));
            Response second = mapper.toResponse(new RuntimeException("boom"));

            @SuppressWarnings("unchecked")
            Map<String, Object> firstBody = (Map<String, Object>) first.getEntity();
            @SuppressWarnings("unchecked")
            Map<String, Object> secondBody = (Map<String, Object>) second.getEntity();

            assertNotNull(firstBody.get("correlationId"));
            assertNotNull(secondBody.get("correlationId"));
            assertTrue(!firstBody.get("correlationId").equals(secondBody.get("correlationId")));
        }
    }
}
