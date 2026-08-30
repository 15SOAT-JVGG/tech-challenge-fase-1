package br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.openapi;

import java.util.UUID;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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
 * Canal voltado ao cliente final, separado das APIs administrativas. O acompanhamento usa a posse
 * do identificador da OS como capacidade de acesso; a decisão sobre o orçamento, por alterar OS e
 * estoque, exige o token assinado e de uso único que a oficina enviou por e-mail.
 */
@Path("/v1/public/work-orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Client — Work Orders",
        description = "Client-facing endpoints to track a work order and decide on its estimate")
public interface PublicWorkOrderControllerDocs {

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

    @POST
    @Path("/estimate-decisions/{token}")
    @Consumes(MediaType.WILDCARD)
    @Operation(summary = "Registrar a decisão do cliente sobre o orçamento",
            description = "Consome o link assinado enviado por e-mail. A aprovação mantém a reserva de estoque e "
                    + "leva a ordem a IN_PROGRESS; a recusa devolve as peças ao estoque, conclui a ordem e "
                    + "preenche cancelledAt. Cada link vale uma única vez e expira em sete dias.")
    @APIResponse(responseCode = "200", description = "Decisão registrada com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = EstimateResponseDto.class)))
    @APIResponse(responseCode = "400", description = "Link de decisão inválido ou adulterado")
    @APIResponse(responseCode = "404", description = "Ordem de serviço ou orçamento não encontrado")
    @APIResponse(responseCode = "409", description = "Orçamento já aprovado ou recusado")
    @APIResponse(responseCode = "410", description = "Link de decisão expirado ou já utilizado")
    @APIResponse(responseCode = "422", description = "Ordem de serviço bloqueada para novas alterações")
    Uni<EstimateResponseDto> decideEstimate(
            @Parameter(name = "token", description = "Signed single-use estimate decision token",
                    required = true, in = ParameterIn.PATH)
            @PathParam("token") String token);
}
