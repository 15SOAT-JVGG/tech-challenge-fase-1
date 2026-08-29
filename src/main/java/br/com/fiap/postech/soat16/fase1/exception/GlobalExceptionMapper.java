package br.com.fiap.postech.soat16.fase1.exception;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);
    private static final int UNPROCESSABLE_ENTITY = 422;

    @Override
    public Response toResponse(Throwable exception) {
        return switch (exception) {
            case ResourceNotFoundException e -> errorResponse(Response.Status.NOT_FOUND, e.getMessage());
            case BusinessException e -> errorResponse(UNPROCESSABLE_ENTITY,
                "Unprocessable Entity", e.getMessage());
            case IllegalArgumentException e -> errorResponse(Response.Status.BAD_REQUEST, e.getMessage());
            case WebApplicationException e -> e.getResponse();
            default -> handleUnexpected(exception);
        };
    }

    private Response handleUnexpected(Throwable exception) {
        // Evita expor detalhes internos e fornece um identificador para rastrear o erro.
        String correlationId = UUID.randomUUID().toString();
        LOG.errorf(exception, "Unexpected error [%s]: %s", correlationId, exception.getMessage());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(Map.of(
                "status", 500,
                "error", "Internal Server Error",
                "message", "Internal server error",
                "correlationId", correlationId,
                "timestamp", OffsetDateTime.now().toString()
            ))
            .build();
    }

    private Response errorResponse(Response.Status status, String message) {
        return errorResponse(status.getStatusCode(), status.getReasonPhrase(), message);
    }

    private Response errorResponse(int statusCode, String error, String message) {
        return Response.status(statusCode)
            .entity(Map.of(
                "status", statusCode,
                "error", error,
                "message", message,
                "timestamp", OffsetDateTime.now().toString()
            ))
            .build();
    }
}
