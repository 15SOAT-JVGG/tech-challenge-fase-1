package br.com.fiap.postech.soat16.fase1.mapper;

import org.mapstruct.Mapper;

import br.com.fiap.postech.soat16.fase1.dto.request.VehicleRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.VehicleResponseDto;
import br.com.fiap.postech.soat16.fase1.model.Customer;
import br.com.fiap.postech.soat16.fase1.model.Vehicle;

@Mapper(componentModel = "cdi")
public interface VehicleMapper {

    default VehicleResponseDto toResponse(Vehicle entity) {
        return new VehicleResponseDto(
                entity.getId(),
                entity.getLicensePlate(),
                entity.getManufacturer(),
                entity.getModel(),
                entity.getColor(),
                entity.getYear(),
                entity.getKmDriven(),
                entity.getType(),
                entity.getCustomer().getId(),
                entity.getCreatedAt()
        );
    }

    default Vehicle toEntity(VehicleRequestDto dto, Customer customer) {
        var entity = new Vehicle();
        buildVehicleEntity(dto, entity);
        entity.setCustomer(customer);
        return entity;
    }

    default void updateEntity(Vehicle vehicle, VehicleRequestDto dto) {
        buildVehicleEntity(dto, vehicle);
    }

    private void buildVehicleEntity(VehicleRequestDto dto, Vehicle entity) {
        entity.setLicensePlate(dto.licensePlate());
        entity.setManufacturer(dto.manufacturer());
        entity.setModel(dto.model());
        entity.setColor(dto.color());
        entity.setYear(dto.year());
        entity.setKmDriven(dto.kmDriven());
        entity.setType(dto.type());
    }
}
