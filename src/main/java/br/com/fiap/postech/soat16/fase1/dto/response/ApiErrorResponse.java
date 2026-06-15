package br.com.fiap.postech.soat16.fase1.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import br.com.fiap.postech.soat16.fase1.exception.ErrorType;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        int status,
        ErrorType type,
        String code,
        String message,
        String path,
        String traceId,
        Instant timestamp
) {
    public static ApiErrorResponse of(
            int status,
            ErrorType type,
            String code,
            String message,
            String path,
            String traceId
    ) {
        return new ApiErrorResponse(status, type, code, message, path, traceId, Instant.now());
    }
}
