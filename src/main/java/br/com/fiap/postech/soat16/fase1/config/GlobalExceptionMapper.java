package br.com.fiap.postech.soat16.fase1.config;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import br.com.fiap.postech.soat16.fase1.dto.response.ApiErrorResponse;
import br.com.fiap.postech.soat16.fase1.exception.AppException;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<AppException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(AppException ex) {

        int status = switch (ex.getType()) {
            case VALIDATION -> 400;
            case UNAUTHORIZED -> 401;
            case NOT_FOUND -> 404;
            case CONFLICT -> 409;
            case BUSINESS -> 422;
        };

        String path = uriInfo != null ? uriInfo.getPath() : null;

        ApiErrorResponse body = ApiErrorResponse.of(
                status,
                ex.getType(),
                ex.getCode(),
                ex.getMessage(),
                path,
                null
        );

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
