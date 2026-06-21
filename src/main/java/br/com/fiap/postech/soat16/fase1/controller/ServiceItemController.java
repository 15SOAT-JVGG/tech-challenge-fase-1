package br.com.fiap.postech.soat16.fase1.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import br.com.fiap.postech.soat16.fase1.controller.docs.ServiceItemControllerDocs;
import br.com.fiap.postech.soat16.fase1.dto.request.ServiceItemRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.ServiceItemResponseDto;
import br.com.fiap.postech.soat16.fase1.service.ServiceItemService;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
@RolesAllowed({"ADMIN", "MECHANIC"})
public class ServiceItemController implements ServiceItemControllerDocs {

    private final ServiceItemService service;

    @Override
    public Uni<List<ServiceItemResponseDto>> listAll() {
        return service.listAll();
    }

    @Override
    public Uni<ServiceItemResponseDto> findById(UUID id) {
        return service.findById(id);
    }

    @RolesAllowed("ADMIN")
    @Override
    public Uni<Response> create(ServiceItemRequestDto dto) {
        return service.create(dto)
            .map(created -> Response.created(URI.create("/admin/services/" + created.id())).entity(created).build());
    }

    @RolesAllowed("ADMIN")
    @Override
    public Uni<ServiceItemResponseDto> update(UUID id, ServiceItemRequestDto dto) {
        return service.update(id, dto);
    }

    @RolesAllowed("ADMIN")
    @Override
    public Uni<Response> delete(UUID id) {
        return service.delete(id)
            .replaceWith(Response.noContent().build());
    }
}
