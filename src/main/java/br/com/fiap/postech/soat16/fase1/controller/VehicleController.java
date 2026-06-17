package br.com.fiap.postech.soat16.fase1.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

import br.com.fiap.postech.soat16.fase1.controller.docs.VehicleControllerDocs;
import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.request.VehicleDto;
import br.com.fiap.postech.soat16.fase1.dto.request.VehicleFilterDto;
import br.com.fiap.postech.soat16.fase1.dto.response.VehicleResponseDto;
import br.com.fiap.postech.soat16.fase1.service.VehicleService;

import io.smallrye.mutiny.Uni;
import lombok.AllArgsConstructor;

import java.util.UUID;

@ApplicationScoped
@AllArgsConstructor
@Path("/v1/vehicle")
public class VehicleController implements VehicleControllerDocs {

    private final VehicleService vehicleService;

    @GET
    @Override
    public Uni<PageableResponseDto<VehicleResponseDto>> listAll(@BeanParam @Valid PageableRequestDto pageable,
                                                                @BeanParam VehicleFilterDto filter) {
        return vehicleService.listAll(pageable, filter);
    }

    @GET
    @Path("/{id}")
    @Override
    public Uni<VehicleResponseDto> findById(@PathParam("id") UUID id) {
        return vehicleService.findById(id);
    }

    @GET
    @Path("/license-plate/{license_plate}")
    @Override
    public Uni<VehicleResponseDto> findByLicensePlate(@PathParam("license_plate") String licensePlate) {
        return vehicleService.findByLicensePlate(licensePlate);
    }

    @POST
    @Override
    public Uni<Response> create(@RequestBody @Valid VehicleDto dto) {
        return vehicleService.create(dto)
                .replaceWith(Response.status(Response.Status.CREATED).build());
    }

    @PUT
    @Path("/{id}")
    @Override
    public Uni<Response> update(@PathParam("id") UUID id,
                                @RequestBody @Valid VehicleDto dto) {
        return vehicleService.update(id, dto)
                .map(updated -> Response.ok(updated).build());
    }

    @DELETE
    @Path("/{id}")
    @Override
    public Uni<Response> delete(@PathParam("id") UUID id) {
        return vehicleService.delete(id)
                .replaceWith(Response.noContent().build());
    }
}
