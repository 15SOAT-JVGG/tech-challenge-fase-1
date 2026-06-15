package br.com.fiap.postech.soat16.fase1.controller;

import br.com.fiap.postech.soat16.fase1.controller.docs.PartControllerDocs;
import br.com.fiap.postech.soat16.fase1.dto.request.PartRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.PartResponseDto;
import br.com.fiap.postech.soat16.fase1.service.PartService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.List;

@ApplicationScoped
@RolesAllowed({"ADMIN", "MECHANIC"})
public class PartController implements PartControllerDocs {

    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    @Override
    public Uni<List<PartResponseDto>> listAll() {
        return partService.listAll();
    }

    @Override
    public Uni<PartResponseDto> findById(Long id) {
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
    public Uni<PartResponseDto> update(Long id, PartRequestDto dto) {
        return partService.update(id, dto);
    }

    @RolesAllowed("ADMIN")
    @Override
    public Uni<PartResponseDto> adjustStock(Long id, int adjustment) {
        return partService.adjustStock(id, adjustment);
    }

    @RolesAllowed("ADMIN")
    @Override
    public Uni<Response> delete(Long id) {
        return partService.delete(id)
            .replaceWith(Response.noContent().build());
    }
}
