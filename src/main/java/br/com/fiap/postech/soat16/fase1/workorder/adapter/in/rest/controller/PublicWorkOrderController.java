package br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.controller;

import java.util.UUID;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.WorkOrderRestMapper;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.EstimateResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.WorkOrderResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.openapi.PublicWorkOrderControllerDocs;
import br.com.fiap.postech.soat16.fase1.workorder.application.WorkOrderService;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
@Path("/v1/public/work-orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
public class PublicWorkOrderController implements PublicWorkOrderControllerDocs {

    private final WorkOrderService service;

    @GET
    @Path("/{id}")
    @Override
    public Uni<WorkOrderResponseDto> track(@PathParam("id") UUID id) {
        return service.findById(id).map(WorkOrderRestMapper::toResponse);
    }

    // CPD-OFF — este adaptador replica as decisões de orçamento para preservar a rota e a política
    // de segurança específicas do cliente.

    @PATCH
    @Path("/{id}/estimate/{estimateId}/approve")
    @Override
    public Uni<EstimateResponseDto> approveEstimate(@PathParam("id") UUID id,
                                                    @PathParam("estimateId") UUID estimateId) {
        return service.approveEstimate(id, estimateId).map(WorkOrderRestMapper::toResponse);
    }

    @PATCH
    @Path("/{id}/estimate/{estimateId}/reject")
    @Override
    public Uni<EstimateResponseDto> rejectEstimate(@PathParam("id") UUID id,
                                                   @PathParam("estimateId") UUID estimateId) {
        return service.rejectEstimate(id, estimateId).map(WorkOrderRestMapper::toResponse);
    }

    // CPD-ON
}
