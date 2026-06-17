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

import br.com.fiap.postech.soat16.fase1.controller.docs.CustomerControllerDocs;
import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.request.CustomerCreateRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.CustomerUpdateRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.CustomerResponseDto;
import br.com.fiap.postech.soat16.fase1.service.CustomerService;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
@Path("/v1/customer")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CustomerController implements CustomerControllerDocs {

    private final CustomerService service;

    @GET
    @Override
    public Uni<PageableResponseDto<CustomerResponseDto>> findAll(@BeanParam @Valid PageableRequestDto pageable) {
        return service.findAll(pageable.getQ(), pageable.getPage(), pageable.getSize());
    }

    @GET
    @Path("/{id}")
    @Override
    public Uni<CustomerResponseDto> findById(@PathParam("id") UUID id) {
        return service.findById(id);
    }

    @GET
    @Path("/by-document/{document}")
    @Override
    public Uni<CustomerResponseDto> findByDocument(@PathParam("document") String document) {
        return service.findByDocument(document);
    }

    @POST
    @Override
    public Uni<Response> create(@RequestBody @Valid CustomerCreateRequestDto dto) {
        return service.create(dto)
                .replaceWith(Response.status(Response.Status.CREATED).build());
    }

    @PUT
    @Path("/{id}")
    @Override
    public Uni<Response> update(@PathParam("id") UUID id, @RequestBody @Valid CustomerUpdateRequestDto dto) {
        return service.update(id, dto)
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
