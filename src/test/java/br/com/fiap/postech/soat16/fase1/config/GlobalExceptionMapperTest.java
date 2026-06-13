package br.com.fiap.postech.soat16.fase1.config;

import br.com.fiap.postech.soat16.fase1.dto.response.ApiErrorResponse;
import br.com.fiap.postech.soat16.fase1.exception.CustomerNotFoundException;
import br.com.fiap.postech.soat16.fase1.exception.DuplicateDocumentException;
import br.com.fiap.postech.soat16.fase1.exception.ErrorType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("GlobalExceptionMapper — Unit Tests")
class GlobalExceptionMapperTest {

    private GlobalExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new GlobalExceptionMapper();
    }

    @Test
    @DisplayName("should map NOT_FOUND exception to HTTP 404")
    void shouldMap404ForNotFoundException() {
        CustomerNotFoundException ex = new CustomerNotFoundException(UUID.randomUUID());

        Response response = mapper.toResponse(ex);

        assertEquals(404, response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals(ErrorType.NOT_FOUND, body.type());
        assertEquals("CUSTOMER_NOT_FOUND", body.code());
    }

    @Test
    @DisplayName("should map CONFLICT exception to HTTP 409")
    void shouldMap409ForConflictException() {
        DuplicateDocumentException ex = new DuplicateDocumentException();

        Response response = mapper.toResponse(ex);

        assertEquals(409, response.getStatus());
        ApiErrorResponse body = (ApiErrorResponse) response.getEntity();
        assertEquals(ErrorType.CONFLICT, body.type());
        assertEquals("DOCUMENT_ALREADY_EXISTS", body.code());
    }
}
