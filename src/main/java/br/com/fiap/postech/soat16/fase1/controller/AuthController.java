package br.com.fiap.postech.soat16.fase1.controller;

import br.com.fiap.postech.soat16.fase1.controller.docs.AuthControllerDocs;
import br.com.fiap.postech.soat16.fase1.dto.request.LoginRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.LoginResponseDto;
import br.com.fiap.postech.soat16.fase1.security.AuthService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

@ApplicationScoped
@RequiredArgsConstructor
@Path("/v1/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;

    @POST
    @Path("/login")
    @PermitAll
    @Override
    public Uni<LoginResponseDto> login(@RequestBody @Valid LoginRequestDto body) {
        return authService.login(body);
    }
}
