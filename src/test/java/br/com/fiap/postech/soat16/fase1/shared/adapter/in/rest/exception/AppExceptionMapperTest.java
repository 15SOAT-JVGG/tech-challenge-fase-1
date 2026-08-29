package br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.customer.domain.exception.CustomerNotFoundException;
import br.com.fiap.postech.soat16.fase1.customer.domain.exception.DuplicateDocumentException;
import br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.dto.ApiErrorResponseDto;
import br.com.fiap.postech.soat16.fase1.shared.domain.exception.ErrorType;

@DisplayName("AppExceptionMapper — Unit Tests")
class AppExceptionMapperTest {

    private AppExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AppExceptionMapper();
    }

    @Test
    @DisplayName("maps NOT_FOUND to HTTP 404")
    void shouldMap404ForNotFoundException() {
        Response response = mapper.toResponse(new CustomerNotFoundException());

        assertEquals(404, response.getStatus());
        ApiErrorResponseDto body = (ApiErrorResponseDto) response.getEntity();
        assertEquals(ErrorType.NOT_FOUND, body.type());
        assertEquals("CUSTOMER_NOT_FOUND", body.code());
    }

    @Test
    @DisplayName("maps CONFLICT to HTTP 409")
    void shouldMap409ForConflictException() {
        Response response = mapper.toResponse(new DuplicateDocumentException());

        assertEquals(409, response.getStatus());
        ApiErrorResponseDto body = (ApiErrorResponseDto) response.getEntity();
        assertEquals(ErrorType.CONFLICT, body.type());
        assertEquals("DOCUMENT_ALREADY_EXISTS", body.code());
    }
}
