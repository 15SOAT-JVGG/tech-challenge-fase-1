package br.com.fiap.postech.soat16.fase1.controller;

import java.util.UUID;

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

import br.com.fiap.postech.soat16.fase1.controller.docs.WorkerControllerDocs;
import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.request.WorkerLoginRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.WorkerRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkerLoginResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkerResponseDto;
import br.com.fiap.postech.soat16.fase1.service.WorkerService;

import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
@Path("/v1/worker")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class WorkerController implements WorkerControllerDocs {

    private final WorkerService service;

    @GET
    @Override
    public PageableResponseDto<WorkerResponseDto> findAll(@BeanParam @Valid PageableRequestDto pageable) {
        return service.findAll(pageable.getQ(), pageable.getPage(), pageable.getSize());
    }

    @GET
    @Path("/{id}")
    @Override
    public WorkerResponseDto findById(@PathParam("id") UUID id) {
        return service.findById(id);
    }

    @POST
    @Override
    public Response create(@RequestBody @Valid WorkerRequestDto dto) {
        service.create(dto);
        return Response.status(Response.Status.CREATED).build();
    }

    @POST
    @Path("/login")
    @Override
    public WorkerLoginResponseDto login(@RequestBody @Valid WorkerLoginRequestDto dto) {
        return service.login(dto);
    }

    @PUT
    @Path("/{id}")
    @Override
    public Response update(@PathParam("id") UUID id, @RequestBody @Valid WorkerRequestDto dto) {
        return Response.ok(service.update(id, dto)).build();
    }

    @DELETE
    @Path("/{id}")
    @Override
    public Response delete(@PathParam("id") UUID id) {
        service.delete(id);
        return Response.noContent().build();
    }
}
