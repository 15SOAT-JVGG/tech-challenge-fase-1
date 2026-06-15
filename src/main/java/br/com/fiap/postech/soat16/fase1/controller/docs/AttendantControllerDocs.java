package br.com.fiap.postech.soat16.fase1.controller.docs;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponseDto;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import br.com.fiap.postech.soat16.fase1.dto.request.AttendantCreateRequest;
import br.com.fiap.postech.soat16.fase1.dto.request.AttendantLoginRequest;
import br.com.fiap.postech.soat16.fase1.dto.request.AttendantUpdateRequest;
import br.com.fiap.postech.soat16.fase1.dto.response.AttendantLoginResponse;
import br.com.fiap.postech.soat16.fase1.dto.response.AttendantResponse;

@Path("/v1/attendant")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Attendant", description = "Attendant management operations")
public interface AttendantControllerDocs {

    @GET
    @Operation(summary = "List attendants", description = "Returns a paginated list of attendants.")
    @APIResponse(responseCode = "200", description = "Attendants retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PageableResponseDto.class)))
    @APIResponse(responseCode = "400", description = "Invalid query parameters")
    PageableResponseDto<AttendantResponse> findAll(@BeanParam @Valid PageableRequestDto pageable);

    @GET
    @Path("/{id}")
    @Operation(summary = "Get attendant by ID", description = "Returns a single attendant by identifier.")
    @APIResponse(responseCode = "200", description = "Attendant found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = AttendantResponse.class)))
    @APIResponse(responseCode = "404", description = "Attendant not found")
    AttendantResponse findById(
            @Parameter(name = "id", description = "Attendant identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id);

    @POST
    @Operation(summary = "Register attendant", description = "Creates a new attendant record.")
    @APIResponse(responseCode = "201", description = "Attendant created successfully")
    @APIResponse(responseCode = "400", description = "Invalid request body")
    @APIResponse(responseCode = "409", description = "Attendant email already exists")
    Response create(
            @RequestBody(description = "Attendant data for registration")
            @Valid AttendantCreateRequest body);

    @POST
    @Path("/login")
    @Operation(summary = "Authenticate attendant", description = "Validates attendant credentials.")
    @APIResponse(responseCode = "200", description = "Attendant authenticated",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = AttendantLoginResponse.class)))
    @APIResponse(responseCode = "401", description = "Invalid credentials or inactive attendant")
    AttendantLoginResponse login(
            @RequestBody(description = "Attendant credentials")
            @Valid AttendantLoginRequest body);

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update attendant", description = "Fully replaces an attendant's data.")
    @APIResponse(responseCode = "200", description = "Attendant updated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = AttendantResponse.class)))
    @APIResponse(responseCode = "404", description = "Attendant not found")
    @APIResponse(responseCode = "409", description = "Attendant email already exists")
    Response update(
            @Parameter(name = "id", description = "Attendant identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id,
            @RequestBody(description = "Updated attendant data")
            @Valid AttendantUpdateRequest body);

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete attendant", description = "Permanently removes an attendant by identifier.")
    @APIResponse(responseCode = "204", description = "Attendant deleted successfully")
    @APIResponse(responseCode = "404", description = "Attendant not found")
    Response delete(
            @Parameter(name = "id", description = "Attendant identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id);
}
