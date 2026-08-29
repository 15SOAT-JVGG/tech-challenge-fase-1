package br.com.fiap.postech.soat16.fase1.part.adapter.in.rest.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import br.com.fiap.postech.soat16.fase1.part.adapter.in.rest.dto.request.PartRequestDto;
import br.com.fiap.postech.soat16.fase1.part.adapter.in.rest.dto.response.PartResponseDto;
import br.com.fiap.postech.soat16.fase1.part.adapter.in.rest.mapper.PartRestMapper;
import br.com.fiap.postech.soat16.fase1.part.adapter.in.rest.openapi.PartControllerDocs;
import br.com.fiap.postech.soat16.fase1.part.application.PartService;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
@RolesAllowed({"ADMIN", "MECHANIC"})
public class PartController implements PartControllerDocs {

    private final PartService partService;

    @Override
    public Uni<List<PartResponseDto>> listAll() {
        return partService.listAll()
                .map(results -> results.stream().map(PartRestMapper::toResponse).toList());
    }

    @Override
    public Uni<PartResponseDto> findById(UUID id) {
        return partService.findById(PartRestMapper.toQuery(id))
                .map(PartRestMapper::toResponse);
    }

    @Override
    public Uni<List<PartResponseDto>> findLowStock() {
        return partService.findLowStock()
                .map(results -> results.stream().map(PartRestMapper::toResponse).toList());
    }

    @RolesAllowed("ADMIN")
    @Override
    public Uni<Response> create(PartRequestDto dto) {
        return partService.create(PartRestMapper.toCreateCommand(dto))
                .map(PartRestMapper::toResponse)
                .map(created -> Response.created(URI.create("/admin/parts/" + created.id()))
                        .entity(created)
                        .build());
    }

    @RolesAllowed("ADMIN")
    @Override
    public Uni<PartResponseDto> update(UUID id, PartRequestDto dto) {
        return partService.update(PartRestMapper.toUpdateCommand(id, dto))
                .map(PartRestMapper::toResponse);
    }

    @RolesAllowed("ADMIN")
    @Override
    public Uni<PartResponseDto> adjustStock(UUID id, int adjustment) {
        return partService.adjustStock(PartRestMapper.toStockCommand(id, adjustment))
                .map(PartRestMapper::toResponse);
    }

    @RolesAllowed("ADMIN")
    @Override
    public Uni<Response> delete(UUID id) {
        return partService.delete(PartRestMapper.toDeleteCommand(id))
                .replaceWith(Response.noContent().build());
    }
}
