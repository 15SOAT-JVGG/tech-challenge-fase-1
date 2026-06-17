package br.com.fiap.postech.soat16.fase1.controller.docs;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import br.com.fiap.postech.soat16.fase1.dto.request.PartRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.PartResponseDto;

import io.smallrye.mutiny.Uni;

@Path("/admin/parts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Parts and Supplies", description = "Parts and supplies management with inventory control")
public interface PartControllerDocs {

    @GET
    @Operation(summary = "List all parts/supplies", description = "Returns all registered parts and supplies.")
    @APIResponse(responseCode = "200", description = "Parts/supplies returned successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PartResponseDto.class, type = org.eclipse.microprofile.openapi.annotations.enums.SchemaType.ARRAY)))
    Uni<List<PartResponseDto>> listAll();

    @GET
    @Path("/{id}")
    @Operation(summary = "Find part/supply by ID", description = "Returns a single part/supply by its identifier.")
    @APIResponse(responseCode = "200", description = "Part/supply found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PartResponseDto.class)))
    @APIResponse(responseCode = "404", description = "Part/supply not found")
    Uni<PartResponseDto> findById(
            @Parameter(name = "id", description = "Part/supply identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") Long id);

    @GET
    @Path("/low-stock")
    @Operation(summary = "List low stock items",
            description = "Returns items whose current stock is at or below the minimum defined for each part.")
    @APIResponse(responseCode = "200", description = "Low stock items returned successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PartResponseDto.class, type = org.eclipse.microprofile.openapi.annotations.enums.SchemaType.ARRAY)))
    Uni<List<PartResponseDto>> findLowStock();

    @POST
    @Operation(summary = "Register new part/supply", description = "Creates a new part/supply record. Restricted to ADMIN.")
    @APIResponse(responseCode = "201", description = "Part/supply created successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PartResponseDto.class)))
    @APIResponse(responseCode = "400", description = "Invalid request body")
    Uni<Response> create(
            @RequestBody(description = "Part/supply data for registration")
            @Valid PartRequestDto body);

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update part/supply", description = "Fully replaces the data of a part/supply. Restricted to ADMIN.")
    @APIResponse(responseCode = "200", description = "Part/supply updated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PartResponseDto.class)))
    @APIResponse(responseCode = "404", description = "Part/supply not found")
    Uni<PartResponseDto> update(
            @Parameter(name = "id", description = "Part/supply identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") Long id,
            @RequestBody(description = "Updated part/supply data")
            @Valid PartRequestDto body);

    @PATCH
    @Path("/{id}/stock")
    @Operation(
            summary = "Adjust stock (positive=inbound, negative=outbound) — ADMIN only",
            description = "Operation restricted to ADMIN. Mechanics consume stock indirectly when adding parts to a service order; " +
                          "manual inbound/outbound adjustments (purchase, return, loss) are an administrative responsibility."
    )
    @APIResponse(responseCode = "200", description = "Stock adjusted successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PartResponseDto.class)))
    @APIResponse(responseCode = "400", description = "Insufficient stock for the requested outbound movement")
    @APIResponse(responseCode = "404", description = "Part/supply not found")
    Uni<PartResponseDto> adjustStock(
            @Parameter(name = "id", description = "Part/supply identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") Long id,
            @Parameter(name = "adjustment", description = "Quantity to adjust (positive=inbound, negative=outbound)",
                    required = true, in = ParameterIn.QUERY)
            @QueryParam("adjustment") int adjustment);

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete part/supply", description = "Permanently removes a part/supply by its identifier. Restricted to ADMIN.")
    @APIResponse(responseCode = "204", description = "Part/supply deleted successfully")
    @APIResponse(responseCode = "404", description = "Part/supply not found")
    Uni<Response> delete(
            @Parameter(name = "id", description = "Part/supply identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") Long id);
}
