package br.com.fiap.postech.soat16.fase1.controller.docs;

import br.com.fiap.postech.soat16.fase1.dto.request.PartRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.PartResponseDto;
import io.smallrye.mutiny.Uni;
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

import java.util.List;

@Path("/admin/parts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Parts and Supplies", description = "Parts and supplies management with inventory control")
public interface PartControllerDocs {

    @GET
    @Operation(summary = "Listar todas as peças/insumos", description = "Retorna todas as peças e insumos cadastrados.")
    @APIResponse(responseCode = "200", description = "Peças/insumos retornados com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PartResponseDto.class, type = org.eclipse.microprofile.openapi.annotations.enums.SchemaType.ARRAY)))
    Uni<List<PartResponseDto>> listAll();

    @GET
    @Path("/{id}")
    @Operation(summary = "Buscar peça/insumo por ID", description = "Retorna uma única peça/insumo pelo identificador.")
    @APIResponse(responseCode = "200", description = "Peça/insumo encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PartResponseDto.class)))
    @APIResponse(responseCode = "404", description = "Peça/insumo não encontrado")
    Uni<PartResponseDto> findById(
            @Parameter(name = "id", description = "Identificador da peça/insumo", required = true, in = ParameterIn.PATH)
            @PathParam("id") Long id);

    @GET
    @Path("/low-stock")
    @Operation(summary = "Listar itens com estoque baixo",
            description = "Retorna itens cujo estoque atual está igual ou abaixo do mínimo definido para cada peça.")
    @APIResponse(responseCode = "200", description = "Itens com estoque baixo retornados com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PartResponseDto.class, type = org.eclipse.microprofile.openapi.annotations.enums.SchemaType.ARRAY)))
    Uni<List<PartResponseDto>> findLowStock();

    @POST
    @Operation(summary = "Cadastrar nova peça/insumo", description = "Cria um novo registro de peça/insumo. Restrito a ADMIN.")
    @APIResponse(responseCode = "201", description = "Peça/insumo criado com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PartResponseDto.class)))
    @APIResponse(responseCode = "400", description = "Corpo da requisição inválido")
    Uni<Response> create(
            @RequestBody(description = "Dados da peça/insumo para cadastro")
            @Valid PartRequestDto body);

    @PUT
    @Path("/{id}")
    @Operation(summary = "Atualizar peça/insumo", description = "Substitui integralmente os dados de uma peça/insumo. Restrito a ADMIN.")
    @APIResponse(responseCode = "200", description = "Peça/insumo atualizado com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PartResponseDto.class)))
    @APIResponse(responseCode = "404", description = "Peça/insumo não encontrado")
    Uni<PartResponseDto> update(
            @Parameter(name = "id", description = "Identificador da peça/insumo", required = true, in = ParameterIn.PATH)
            @PathParam("id") Long id,
            @RequestBody(description = "Dados atualizados da peça/insumo")
            @Valid PartRequestDto body);

    @PATCH
    @Path("/{id}/stock")
    @Operation(
            summary = "Ajustar estoque (positivo=entrada, negativo=saída) — somente ADMIN",
            description = "Operação restrita a ADMIN. Mecânicos consomem estoque indiretamente ao adicionar peças à OS; " +
                          "ajustes manuais de entrada/saída (compra, devolução, perda) são responsabilidade administrativa."
    )
    @APIResponse(responseCode = "200", description = "Estoque ajustado com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = PartResponseDto.class)))
    @APIResponse(responseCode = "400", description = "Estoque insuficiente para a saída solicitada")
    @APIResponse(responseCode = "404", description = "Peça/insumo não encontrado")
    Uni<PartResponseDto> adjustStock(
            @Parameter(name = "id", description = "Identificador da peça/insumo", required = true, in = ParameterIn.PATH)
            @PathParam("id") Long id,
            @Parameter(name = "adjustment", description = "Quantidade a ajustar (positivo=entrada, negativo=saída)",
                    required = true, in = ParameterIn.QUERY)
            @QueryParam("adjustment") int adjustment);

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Excluir peça/insumo", description = "Remove permanentemente uma peça/insumo pelo identificador. Restrito a ADMIN.")
    @APIResponse(responseCode = "204", description = "Peça/insumo excluído com sucesso")
    @APIResponse(responseCode = "404", description = "Peça/insumo não encontrado")
    Uni<Response> delete(
            @Parameter(name = "id", description = "Identificador da peça/insumo", required = true, in = ParameterIn.PATH)
            @PathParam("id") Long id);
}
