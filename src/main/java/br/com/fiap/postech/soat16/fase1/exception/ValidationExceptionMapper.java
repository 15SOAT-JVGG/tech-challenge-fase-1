package br.com.fiap.postech.soat16.fase1.exception;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<String> errors = exception.getConstraintViolations().stream()
            .map(cv -> {
                String field = cv.getPropertyPath().toString();
                if (field.contains(".")) {
                    field = field.substring(field.lastIndexOf('.') + 1);
                }
                return field + ": " + cv.getMessage();
            })
            .sorted()
            .toList();

        return Response.status(Response.Status.BAD_REQUEST)
            .entity(Map.of(
                "status", 400,
                "error", "Validation Error",
                "violations", errors,
                "timestamp", OffsetDateTime.now().toString()
            ))
            .build();
    }
}
