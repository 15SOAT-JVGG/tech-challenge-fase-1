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

import br.com.fiap.postech.soat16.fase1.controller.docs.AttendantControllerDocs;
import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableRequest;
import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponse;
import br.com.fiap.postech.soat16.fase1.dto.request.AttendantCreateRequest;
import br.com.fiap.postech.soat16.fase1.dto.request.AttendantLoginRequest;
import br.com.fiap.postech.soat16.fase1.dto.request.AttendantUpdateRequest;
import br.com.fiap.postech.soat16.fase1.dto.response.AttendantLoginResponse;
import br.com.fiap.postech.soat16.fase1.dto.response.AttendantResponse;
import br.com.fiap.postech.soat16.fase1.service.AttendantService;

import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
@Path("/v1/attendant")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AttendantController implements AttendantControllerDocs {

    private final AttendantService service;

    @GET
    @Override
    public PageableResponse<AttendantResponse> findAll(@BeanParam @Valid PageableRequest pageable) {
        return service.findAll(pageable.getQ(), pageable.getPage(), pageable.getSize());
    }

    @GET
    @Path("/{id}")
    @Override
    public AttendantResponse findById(@PathParam("id") UUID id) {
        return service.findById(id);
    }

    @POST
    @Override
    public Response create(@RequestBody @Valid AttendantCreateRequest dto) {
        service.create(dto);
        return Response.status(Response.Status.CREATED).build();
    }

    @POST
    @Path("/login")
    @Override
    public AttendantLoginResponse login(@RequestBody @Valid AttendantLoginRequest dto) {
        return service.login(dto);
    }

    @PUT
    @Path("/{id}")
    @Override
    public Response update(@PathParam("id") UUID id, @RequestBody @Valid AttendantUpdateRequest dto) {
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
