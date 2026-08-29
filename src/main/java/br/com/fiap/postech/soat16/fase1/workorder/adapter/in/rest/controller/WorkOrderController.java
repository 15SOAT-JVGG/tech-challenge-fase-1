package br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.controller;

import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

import br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.WorkOrderRestMapper;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.request.EstimateRequestDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.request.WorkOrderCloseRequestDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.request.WorkOrderRequestDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.request.WorkOrderServiceRequestDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.request.WorkOrderStatusUpdateRequestDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.EstimateResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.WorkOrderMetricsResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.WorkOrderResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.openapi.WorkOrderControllerDocs;
import br.com.fiap.postech.soat16.fase1.workorder.application.WorkOrderService;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
@Path("/v1/work-orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMIN", "MECHANIC"})
public class WorkOrderController implements WorkOrderControllerDocs {

    private final WorkOrderService service;

    @GET
    @Override
    public Uni<PageableResponseDto<WorkOrderResponseDto>> findAll(@BeanParam @Valid PageableRequestDto pageable) {
        return service.findAll(pageable.getQ(), pageable.getPage(), pageable.getSize())
                .map(WorkOrderRestMapper::toResponse);
    }

    @GET
    @Path("/metrics/average-execution-time")
    @RolesAllowed("ADMIN")
    @Override
    public Uni<WorkOrderMetricsResponseDto> averageExecutionTime() {
        return service.averageExecutionTime().map(WorkOrderRestMapper::toResponse);
    }

    @GET
    @Path("/{id}")
    @Override
    public Uni<WorkOrderResponseDto> findById(@PathParam("id") UUID id) {
        return service.findById(id).map(WorkOrderRestMapper::toResponse);
    }

    @POST
    @Override
    public Uni<Response> create(@RequestBody @Valid WorkOrderRequestDto dto) {
        return service.create(WorkOrderRestMapper.toCommand(dto))
                .replaceWith(Response.status(Response.Status.CREATED).build());
    }

    @PATCH
    @Path("/{id}/status")
    @Override
    public Uni<WorkOrderResponseDto> updateStatus(@PathParam("id") UUID id,
                                                   @RequestBody @Valid WorkOrderStatusUpdateRequestDto dto) {
        return service.updateStatus(id, WorkOrderRestMapper.toCommand(dto))
                .map(WorkOrderRestMapper::toResponse);
    }

    @POST
    @Path("/{id}/estimate")
    @Override
    public Uni<Response> createEstimate(@PathParam("id") UUID id,
                                         @RequestBody @Valid EstimateRequestDto dto) {
        return service.createEstimate(id, WorkOrderRestMapper.toCommand(dto))
                .map(WorkOrderRestMapper::toResponse)
                .map(created -> Response.status(Response.Status.CREATED).entity(created).build());
    }

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

    @POST
    @Path("/{id}/services")
    @Override
    public Uni<Response> addService(@PathParam("id") UUID id,
                                     @RequestBody @Valid WorkOrderServiceRequestDto dto) {
        return service.addService(id, WorkOrderRestMapper.toCommand(dto))
                .map(WorkOrderRestMapper::toResponse)
                .map(created -> Response.status(Response.Status.CREATED).entity(created).build());
    }

    @PATCH
    @Path("/{id}/close")
    @Override
    public Uni<WorkOrderResponseDto> close(@PathParam("id") UUID id,
                                            @RequestBody @Valid WorkOrderCloseRequestDto dto) {
        return service.close(id, WorkOrderRestMapper.toCommand(dto))
                .map(WorkOrderRestMapper::toResponse);
    }
}
