package br.com.fiap.postech.soat16.fase1.controller.docs;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableRequest;
import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponse;
import br.com.fiap.postech.soat16.fase1.dto.request.CustomerCreateRequest;
import br.com.fiap.postech.soat16.fase1.dto.request.CustomerUpdateRequest;
import br.com.fiap.postech.soat16.fase1.dto.response.CustomerResponse;
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

import java.util.UUID;

@Path("/v1/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Customer", description = "Customer management operations")
public interface CustomerControllerDocs {

    @GET
    @Operation(summary = "List customers", description = "Returns a paginated list of customers.")
    @APIResponse(responseCode = "200", description = "Customers retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PageableResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid query parameters")
    Uni<PageableResponse<CustomerResponse>> findAll(@BeanParam @Valid PageableRequest pageable);

    @GET
    @Path("/{id}")
    @Operation(summary = "Get customer by ID", description = "Returns a single customer by internal identifier.")
    @APIResponse(responseCode = "200", description = "Customer found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = CustomerResponse.class)))
    @APIResponse(responseCode = "404", description = "Customer not found")
    Uni<CustomerResponse> findById(
            @Parameter(name = "id", description = "Customer internal identifier (UUID)", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id);

    @GET
    @Path("/by-document/{document}")
    @Operation(summary = "Find customer by document", description = "Returns a customer identified by CPF or CNPJ. Accepts both masked (e.g. 529.982.247-25) and unmasked (e.g. 52998224725) formats.")
    @APIResponse(responseCode = "200", description = "Customer found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = CustomerResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid CPF/CNPJ format")
    @APIResponse(responseCode = "404", description = "Customer not found")
    Uni<CustomerResponse> findByDocument(
            @Parameter(name = "document", description = "Customer CPF or CNPJ (with or without mask)", required = true, in = ParameterIn.PATH)
            @PathParam("document") String document);

    @POST
    @Operation(summary = "Register customer", description = "Creates a new customer record.")
    @APIResponse(responseCode = "201", description = "Customer created successfully")
    @APIResponse(responseCode = "400", description = "Invalid request body")
    @APIResponse(responseCode = "409", description = "Document already registered")
    Uni<Response> create(
            @RequestBody(required = true, description = "Customer data for registration")
            @Valid CustomerCreateRequest body);

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update customer", description = "Fully replaces a customer's data. Document (CPF/CNPJ) cannot be changed.")
    @APIResponse(responseCode = "200", description = "Customer updated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = CustomerResponse.class)))
    @APIResponse(responseCode = "404", description = "Customer not found")
    Uni<Response> update(
            @Parameter(name = "id", description = "Customer identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id,
            @RequestBody(required = true, description = "Updated customer data. Document (CPF/CNPJ) is not updatable.")
            @Valid CustomerUpdateRequest body);

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete customer", description = "Permanently removes a customer by identifier.")
    @APIResponse(responseCode = "204", description = "Customer deleted successfully")
    @APIResponse(responseCode = "404", description = "Customer not found")
    Uni<Response> delete(
            @Parameter(name = "id", description = "Customer identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id);
}
