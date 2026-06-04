package br.com.fiap.postech.soat16.fase1.controller.docs;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.request.VehicleRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.VehicleResponseDto;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
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

@Path("/v1/vehicle")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Vehicle", description = "Vehicle management operations")
public interface VehicleControllerDocs {

    @GET
    @Operation(summary = "List vehicles", description = "Returns a paginated list of vehicles.")
    @APIResponse(responseCode = "200", description = "Vehicles retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PageableResponseDto.class)))
    @APIResponse(responseCode = "400", description = "Invalid query parameters")
    Uni<PageableResponseDto<VehicleResponseDto>> listAll(@BeanParam @Valid PageableRequestDto pageable);

    @GET
    @Path("/{id}")
    @Operation(summary = "Get vehicle by ID", description = "Returns a single vehicle by identifier.")
    @APIResponse(responseCode = "200", description = "Vehicle found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = VehicleResponseDto.class)))
    @APIResponse(responseCode = "404", description = "Vehicle not found")
    Uni<VehicleResponseDto> findById(
            @Parameter(name = "id", description = "Vehicle identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") Long id);

    @POST
    @Operation(summary = "Register vehicle", description = "Creates a new vehicle record.")
    @APIResponse(responseCode = "201", description = "Vehicle created successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = VehicleResponseDto.class)))
    @APIResponse(responseCode = "400", description = "Invalid request body")
    @APIResponse(responseCode = "409", description = "License plate already registered")
    Uni<Response> create(
            @RequestBody(required = true, description = "Vehicle data for registration")
            @Valid VehicleRequestDto body);

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update vehicle", description = "Fully replaces a vehicle's data.")
    @APIResponse(responseCode = "200", description = "Vehicle updated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = VehicleResponseDto.class)))
    @APIResponse(responseCode = "404", description = "Vehicle not found")
    Uni<Response> update(
            @Parameter(name = "id", description = "Vehicle identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") Long id,
            @RequestBody(required = true, description = "Updated vehicle data")
            @Valid VehicleRequestDto body);

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete vehicle", description = "Permanently removes a vehicle by identifier.")
    @APIResponse(responseCode = "204", description = "Vehicle deleted successfully")
    @APIResponse(responseCode = "404", description = "Vehicle not found")
    Uni<Response> delete(
            @Parameter(name = "id", description = "Vehicle identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") Long id);
}