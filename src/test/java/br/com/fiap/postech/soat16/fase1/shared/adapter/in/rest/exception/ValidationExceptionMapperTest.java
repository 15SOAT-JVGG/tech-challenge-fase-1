package br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidationExceptionMapper — Unit Tests")
class ValidationExceptionMapperTest {

    private ValidationExceptionMapper mapper;

    @Mock
    private ConstraintViolation<Object> firstViolation;

    @Mock
    private ConstraintViolation<Object> secondViolation;

    @Mock
    private Path firstPath;

    @Mock
    private Path secondPath;

    @BeforeEach
    void setUp() {
        mapper = new ValidationExceptionMapper();
    }

    private ConstraintViolationException exceptionWith(
            ConstraintViolation<?>... violations
    ) {
        return new ConstraintViolationException(Set.of(violations));
    }

    @Test
    @DisplayName("maps one violation as field and message")
    void mapsSingleViolation() {
        when(firstPath.toString()).thenReturn("name");
        when(firstViolation.getPropertyPath()).thenReturn(firstPath);
        when(firstViolation.getMessage()).thenReturn("must not be blank");

        Response response = mapper.toResponse(exceptionWith(firstViolation));

        assertEquals(400, response.getStatus());
        Map<String, Object> body = responseBody(response);
        assertEquals("Validation Error", body.get("error"));
        assertNotNull(body.get("timestamp"));
        assertEquals(
                List.of("name: must not be blank"),
                body.get("violations"));
    }

    @Test
    @DisplayName("keeps only the leaf field name")
    void stripsNestedPropertyPathPrefix() {
        when(firstPath.toString()).thenReturn("createVehicle.arg0.licensePlate");
        when(firstViolation.getPropertyPath()).thenReturn(firstPath);
        when(firstViolation.getMessage()).thenReturn("Invalid license plate");

        Response response = mapper.toResponse(exceptionWith(firstViolation));

        assertEquals(
                List.of("licensePlate: Invalid license plate"),
                responseBody(response).get("violations"));
    }

    @Test
    @DisplayName("sorts multiple violations")
    void sortsMultipleViolations() {
        when(firstPath.toString()).thenReturn("year");
        when(firstViolation.getPropertyPath()).thenReturn(firstPath);
        when(firstViolation.getMessage()).thenReturn("must be positive");
        when(secondPath.toString()).thenReturn("kmDriven");
        when(secondViolation.getPropertyPath()).thenReturn(secondPath);
        when(secondViolation.getMessage()).thenReturn("must be >= 0");

        Response response = mapper.toResponse(
                exceptionWith(firstViolation, secondViolation));

        assertEquals(
                List.of("kmDriven: must be >= 0", "year: must be positive"),
                responseBody(response).get("violations"));
    }

    @Test
    @DisplayName("returns an empty violations list")
    void emptyViolationsList() {
        Response response = mapper.toResponse(exceptionWith());

        assertEquals(List.of(), responseBody(response).get("violations"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> responseBody(Response response) {
        return (Map<String, Object>) response.getEntity();
    }
}
