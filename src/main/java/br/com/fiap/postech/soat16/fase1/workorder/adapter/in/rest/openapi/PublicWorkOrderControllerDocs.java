package br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.openapi;

import java.util.UUID;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.EstimateResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.WorkOrderResponseDto;

import io.smallrye.mutiny.Uni;

/**
 * Canal voltado ao cliente final, separado das APIs administrativas. Nesta versão (MVP), a posse do
 * identificador da OS funciona como capacidade de acesso; uma evolução
 * autenticaria o cliente ou usaria um token de acesso por OS.
 */
@Path("/v1/public/work-orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Client — Work Orders",
        description = "Client-facing endpoints to track a work order and decide on its estimate")
public interface PublicWorkOrderControllerDocs {

    // CPD-OFF — por contrato, as anotações OpenAPI espelham as do WorkOrderControllerDocs.

    @GET
    @Path("/{id}")
    @Operation(summary = "Track work order",
            description = "Returns the current status and details of a work order for the client.")
    @APIResponse(responseCode = "200", description = "Work order found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = WorkOrderResponseDto.class)))
    @APIResponse(responseCode = "404", description = "Work order not found")
    Uni<WorkOrderResponseDto> track(
            @Parameter(name = "id", description = "Work order identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id);

    @PATCH
    @Path("/{id}/estimate/{estimateId}/approve")
    @Operation(summary = "Approve estimate (client)",
            description = "Client authorizes the estimate. Reserves the parts in stock and advances the work "
                    + "order to IN_PROGRESS.")
    @APIResponse(responseCode = "200", description = "Estimate approved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = EstimateResponseDto.class)))
    @APIResponse(responseCode = "404", description = "Work order or estimate not found")
    @APIResponse(responseCode = "409", description = "Estimate already approved or rejected")
    @APIResponse(responseCode = "422", description = "Insufficient stock for one of the parts")
    Uni<EstimateResponseDto> approveEstimate(
            @Parameter(name = "id", description = "Work order identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id,
            @Parameter(name = "estimateId", description = "Estimate identifier", required = true,
                    in = ParameterIn.PATH)
            @PathParam("estimateId") UUID estimateId);

    @PATCH
    @Path("/{id}/estimate/{estimateId}/reject")
    @Operation(summary = "Recusar orçamento pelo canal do cliente",
            description = "A recusa conclui a ordem de serviço, preenche cancelledAt e bloqueia novas alterações.")
    @APIResponse(responseCode = "200", description = "Orçamento recusado com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = EstimateResponseDto.class)))
    @APIResponse(responseCode = "404", description = "Ordem de serviço ou orçamento não encontrado")
    @APIResponse(responseCode = "409", description = "Orçamento já aprovado ou recusado")
    Uni<EstimateResponseDto> rejectEstimate(
            @Parameter(name = "id", description = "Work order identifier", required = true, in = ParameterIn.PATH)
            @PathParam("id") UUID id,
            @Parameter(name = "estimateId", description = "Estimate identifier", required = true,
                    in = ParameterIn.PATH)
            @PathParam("estimateId") UUID estimateId);

    // CPD-ON
}
