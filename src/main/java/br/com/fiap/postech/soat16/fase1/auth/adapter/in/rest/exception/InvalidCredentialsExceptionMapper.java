package br.com.fiap.postech.soat16.fase1.auth.adapter.in.rest.exception;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import br.com.fiap.postech.soat16.fase1.auth.domain.exception.InvalidCredentialsException;

@Provider
public class InvalidCredentialsExceptionMapper
        implements ExceptionMapper<InvalidCredentialsException> {

    @Override
    public Response toResponse(InvalidCredentialsException exception) {
        return new NotAuthorizedException(exception.getMessage(), "Bearer").getResponse();
    }
}
