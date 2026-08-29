package br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.controller;

import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

import br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.dto.request.VehicleFilterDto;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.dto.request.VehicleRequestDto;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.dto.response.VehicleResponseDto;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.mapper.VehicleRestMapper;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.openapi.VehicleControllerDocs;
import br.com.fiap.postech.soat16.fase1.vehicle.application.VehicleService;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
@Path("/v1/vehicle")
@RolesAllowed({"ADMIN", "MECHANIC"})
public class VehicleController implements VehicleControllerDocs {

    private final VehicleService vehicleService;

    @GET
    @Override
    public Uni<PageableResponseDto<VehicleResponseDto>> listAll(
            @BeanParam @Valid PageableRequestDto pageable,
            @BeanParam VehicleFilterDto filter) {
        return vehicleService.listAll(VehicleRestMapper.toQuery(pageable, filter))
                .map(VehicleRestMapper::toResponse);
    }

    @GET
    @Path("/{id}")
    @Override
    public Uni<VehicleResponseDto> findById(@PathParam("id") UUID id) {
        return vehicleService.findById(id).map(VehicleRestMapper::toResponse);
    }

    @GET
    @Path("/by-license-plate/{license_plate}")
    @Override
    public Uni<VehicleResponseDto> findByLicensePlate(
            @PathParam("license_plate") String licensePlate) {
        return vehicleService.findByLicensePlate(licensePlate).map(VehicleRestMapper::toResponse);
    }

    @POST
    @RolesAllowed("ADMIN")
    @Override
    public Uni<Response> create(@RequestBody @Valid VehicleRequestDto dto) {
        return vehicleService.create(VehicleRestMapper.toCreateCommand(dto))
                .replaceWith(Response.status(Response.Status.CREATED).build());
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Override
    public Uni<Response> update(@PathParam("id") UUID id,
            @RequestBody @Valid VehicleRequestDto dto) {
        return vehicleService.update(VehicleRestMapper.toUpdateCommand(id, dto))
                .map(VehicleRestMapper::toResponse)
                .map(updated -> Response.ok(updated).build());
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Override
    public Uni<Response> delete(@PathParam("id") UUID id) {
        return vehicleService.delete(id)
                .replaceWith(Response.noContent().build());
    }
}
