package br.com.fiap.postech.soat16.fase1.controller.docs;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import br.com.fiap.postech.soat16.fase1.dto.request.LoginRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.LoginResponseDto;

import io.smallrye.mutiny.Uni;

@Path("/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "Authentication and token issuing operations")
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface AuthControllerDocs {

    @POST
    @Path("/login")
    @SecurityRequirements
    @Operation(summary = "Authenticate user",
            description = "Validates credentials and returns a signed JWT for use as a Bearer token.")
    @APIResponse(responseCode = "200", description = "Authentication successful",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = LoginResponseDto.class)))
    @APIResponse(responseCode = "400", description = "Invalid request body")
    @APIResponse(responseCode = "401", description = "Invalid credentials")
    Uni<LoginResponseDto> login(
            @RequestBody(description = "User credentials")
            @Valid LoginRequestDto body);
}
