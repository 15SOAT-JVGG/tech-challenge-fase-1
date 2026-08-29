package br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.controller;

import java.util.UUID;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
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

import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

import br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.dto.request.WorkerLoginRequestDto;
import br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.dto.request.WorkerRequestDto;
import br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.dto.response.WorkerLoginResponseDto;
import br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.dto.response.WorkerResponseDto;
import br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.mapper.WorkerRestMapper;
import br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.openapi.WorkerControllerDocs;
import br.com.fiap.postech.soat16.fase1.worker.application.WorkerService;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
@Path("/v1/worker")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class WorkerController implements WorkerControllerDocs {

    private final WorkerService service;

    @GET
    @Override
    public Uni<PageableResponseDto<WorkerResponseDto>> findAll(
            @BeanParam @Valid PageableRequestDto pageable) {
        return service.findAll(pageable.getQ(), pageable.getPage(), pageable.getSize())
                .map(WorkerRestMapper::toResponse);
    }

    @GET
    @Path("/{id}")
    @Override
    public Uni<WorkerResponseDto> findById(@PathParam("id") UUID id) {
        return service.findById(id).map(WorkerRestMapper::toResponse);
    }

    @POST
    @Override
    public Uni<Response> create(@RequestBody @Valid WorkerRequestDto dto) {
        return service.create(WorkerRestMapper.toCreateCommand(dto))
                .replaceWith(Response.status(Response.Status.CREATED).build());
    }

    @POST
    @Path("/login")
    @PermitAll
    @Override
    public Uni<WorkerLoginResponseDto> login(@RequestBody @Valid WorkerLoginRequestDto dto) {
        return service.login(WorkerRestMapper.toLoginCommand(dto))
                .map(WorkerRestMapper::toResponse);
    }

    @PUT
    @Path("/{id}")
    @Override
    public Uni<Response> update(@PathParam("id") UUID id,
            @RequestBody @Valid WorkerRequestDto dto) {
        return service.update(WorkerRestMapper.toUpdateCommand(id, dto))
                .map(WorkerRestMapper::toResponse)
                .map(updated -> Response.ok(updated).build());
    }

    @DELETE
    @Path("/{id}")
    @Override
    public Uni<Response> delete(@PathParam("id") UUID id) {
        return service.delete(id)
                .replaceWith(Response.noContent().build());
    }
}
