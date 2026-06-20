package br.com.fiap.postech.soat16.fase1.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import br.com.fiap.postech.soat16.fase1.controller.docs.PartControllerDocs;
import br.com.fiap.postech.soat16.fase1.dto.request.PartRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.PartResponseDto;
import br.com.fiap.postech.soat16.fase1.service.PartService;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
@RolesAllowed({"ADMIN", "MECHANIC"})
public class PartController implements PartControllerDocs {

    private final PartService partService;

    @Override
    public Uni<List<PartResponseDto>> listAll() {
        return partService.listAll();
    }

    @Override
    public Uni<PartResponseDto> findById(UUID id) {
        return partService.findById(id);
    }

    @Override
    public Uni<List<PartResponseDto>> findLowStock() {
        return partService.findLowStock();
    }

    @RolesAllowed("ADMIN")
    @Override
    public Uni<Response> create(PartRequestDto dto) {
        return partService.create(dto)
            .map(created -> Response.created(URI.create("/admin/parts/" + created.id())).entity(created).build());
    }

    @RolesAllowed("ADMIN")
    @Override
    public Uni<PartResponseDto> update(UUID id, PartRequestDto dto) {
        return partService.update(id, dto);
    }

    @RolesAllowed("ADMIN")
    @Override
    public Uni<PartResponseDto> adjustStock(UUID id, int adjustment) {
        return partService.adjustStock(id, adjustment);
    }

    @RolesAllowed("ADMIN")
    @Override
    public Uni<Response> delete(UUID id) {
        return partService.delete(id)
            .replaceWith(Response.noContent().build());
    }
}
