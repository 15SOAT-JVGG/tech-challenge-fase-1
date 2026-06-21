package br.com.fiap.postech.soat16.fase1.exception;

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

    private ConstraintViolationException exceptionWith(ConstraintViolation<?>... violations) {
        return new ConstraintViolationException(Set.of(violations));
    }

    @Test
    @DisplayName("maps to HTTP 400 with one violation formatted as 'field: message'")
    void mapsSingleViolation() {
        when(firstPath.toString()).thenReturn("name");
        when(firstViolation.getPropertyPath()).thenReturn(firstPath);
        when(firstViolation.getMessage()).thenReturn("must not be blank");

        Response response = mapper.toResponse(exceptionWith(firstViolation));

        assertEquals(400, response.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        assertEquals(400, body.get("status"));
        assertEquals("Validation Error", body.get("error"));
        assertNotNull(body.get("timestamp"));
        @SuppressWarnings("unchecked")
        List<String> violations = (List<String>) body.get("violations");
        assertEquals(List.of("name: must not be blank"), violations);
    }

    @Test
    @DisplayName("strips the leading path and keeps only the leaf field name when the path has a prefix")
    void stripsNestedPropertyPathPrefix() {
        when(firstPath.toString()).thenReturn("createVehicle.arg0.licensePlate");
        when(firstViolation.getPropertyPath()).thenReturn(firstPath);
        when(firstViolation.getMessage()).thenReturn("Invalid license plate");

        Response response = mapper.toResponse(exceptionWith(firstViolation));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        @SuppressWarnings("unchecked")
        List<String> violations = (List<String>) body.get("violations");
        assertEquals(List.of("licensePlate: Invalid license plate"), violations);
    }

    @Test
    @DisplayName("sorts multiple violations alphabetically by their formatted message")
    void sortsMultipleViolations() {
        when(firstPath.toString()).thenReturn("year");
        when(firstViolation.getPropertyPath()).thenReturn(firstPath);
        when(firstViolation.getMessage()).thenReturn("must be positive");

        when(secondPath.toString()).thenReturn("kmDriven");
        when(secondViolation.getPropertyPath()).thenReturn(secondPath);
        when(secondViolation.getMessage()).thenReturn("must be >= 0");

        Response response = mapper.toResponse(exceptionWith(firstViolation, secondViolation));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        @SuppressWarnings("unchecked")
        List<String> violations = (List<String>) body.get("violations");
        assertEquals(List.of("kmDriven: must be >= 0", "year: must be positive"), violations);
    }

    @Test
    @DisplayName("returns an empty violations list when there are no constraint violations")
    void emptyViolationsList() {
        Response response = mapper.toResponse(exceptionWith());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();
        @SuppressWarnings("unchecked")
        List<String> violations = (List<String>) body.get("violations");
        assertEquals(List.of(), violations);
    }
}
