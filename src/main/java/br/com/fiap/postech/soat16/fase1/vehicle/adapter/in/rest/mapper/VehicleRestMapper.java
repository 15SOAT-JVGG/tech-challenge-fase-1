package br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.mapper;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.shared.application.result.PagedResult;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.dto.request.VehicleFilterDto;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.dto.request.VehicleRequestDto;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.dto.response.VehicleResponseDto;
import br.com.fiap.postech.soat16.fase1.vehicle.application.command.CreateVehicleCommand;
import br.com.fiap.postech.soat16.fase1.vehicle.application.command.UpdateVehicleCommand;
import br.com.fiap.postech.soat16.fase1.vehicle.application.query.VehicleQuery;
import br.com.fiap.postech.soat16.fase1.vehicle.application.result.VehicleResult;

public final class VehicleRestMapper {

    private VehicleRestMapper() {
    }

    public static VehicleQuery toQuery(
            PageableRequestDto pageable, VehicleFilterDto filter) {
        return VehicleQuery.of(
                pageable.getPage(),
                pageable.getSize(),
                pageable.getSortParameters(),
                filter.getLicensePlate(),
                filter.getManufacturer(),
                filter.getModel());
    }

    public static CreateVehicleCommand toCreateCommand(VehicleRequestDto request) {
        return new CreateVehicleCommand(
                request.customerId(),
                request.licensePlate(),
                request.manufacturer(),
                request.model(),
                request.color(),
                request.year(),
                request.kmDriven(),
                request.type());
    }

    public static UpdateVehicleCommand toUpdateCommand(UUID id, VehicleRequestDto request) {
        return new UpdateVehicleCommand(
                id,
                request.licensePlate(),
                request.manufacturer(),
                request.model(),
                request.color(),
                request.year(),
                request.kmDriven(),
                request.type());
    }

    public static PageableResponseDto<VehicleResponseDto> toResponse(
            PagedResult<VehicleResult> page) {
        return PageableResponseDto.of(
                page.content().stream().map(VehicleRestMapper::toResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements());
    }

    public static VehicleResponseDto toResponse(VehicleResult result) {
        return new VehicleResponseDto(
                result.id(),
                result.licensePlate(),
                result.manufacturer(),
                result.model(),
                result.color(),
                result.year(),
                result.kmDriven(),
                result.type(),
                result.customerId(),
                result.createdAt());
    }
}
