package br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.openapi;

import java.util.UUID;

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

import br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.dto.request.WorkerLoginRequestDto;
import br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.dto.request.WorkerRequestDto;
import br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.dto.response.WorkerLoginResponseDto;
import br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.dto.response.WorkerResponseDto;

import io.smallrye.mutiny.Uni;

@Path("/v1/worker")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Worker", description = "Worker management operations")
public interface WorkerControllerDocs {

    @GET
    @Operation(summary = "List workers", description = "Returns a paginated list of workers.")
    @APIResponse(responseCode = "200", description = "Workers retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PageableResponseDto.class)))
    @APIResponse(responseCode = "400", description = "Invalid query parameters")
    Uni<PageableResponseDto<WorkerResponseDto>> findAll(@BeanParam @Valid PageableRequestDto pageable);

    @GET
    @Path("/{id}")
    @Operation(summary = "Get worker by ID", description = "Returns a single worker by identifier.")
    @APIResponse(responseCode = "200", description = "Worker found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = WorkerResponseDto.class)))
    @APIResponse(responseCode = "404", description = "Worker not found")
    Uni<WorkerResponseDto> findById(
            @Parameter(name = "id", description = "Worker identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id);

    @POST
    @Operation(summary = "Register worker", description = "Creates a new worker record.")
    @APIResponse(responseCode = "201", description = "Worker created successfully")
    @APIResponse(responseCode = "400", description = "Invalid request body")
    @APIResponse(responseCode = "409", description = "Worker email already exists")
    Uni<Response> create(
            @RequestBody(description = "Worker data for registration")
            @Valid WorkerRequestDto body);

    @POST
    @Path("/login")
    @Operation(summary = "Authenticate worker", description = "Validates worker credentials.")
    @APIResponse(responseCode = "200", description = "Worker authenticated",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = WorkerLoginResponseDto.class)))
    @APIResponse(responseCode = "401", description = "Invalid credentials or inactive worker")
    Uni<WorkerLoginResponseDto> login(
            @RequestBody(description = "Worker credentials")
            @Valid WorkerLoginRequestDto body);

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update worker", description = "Fully replaces an worker's data.")
    @APIResponse(responseCode = "200", description = "Worker updated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = WorkerResponseDto.class)))
    @APIResponse(responseCode = "404", description = "Worker not found")
    @APIResponse(responseCode = "409", description = "Worker email already exists")
    Uni<Response> update(
            @Parameter(name = "id", description = "Worker identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id,
            @RequestBody(description = "Updated worker data")
            @Valid WorkerRequestDto body);

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete worker", description = "Permanently removes an worker by identifier.")
    @APIResponse(responseCode = "204", description = "Worker deleted successfully")
    @APIResponse(responseCode = "404", description = "Worker not found")
    Uni<Response> delete(
            @Parameter(name = "id", description = "Worker identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id);
}
