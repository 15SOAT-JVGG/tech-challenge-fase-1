package br.com.fiap.postech.soat16.fase1.mapper;

import br.com.fiap.postech.soat16.fase1.dto.request.VehicleDto;
import br.com.fiap.postech.soat16.fase1.dto.response.VehicleResponseDto;
import br.com.fiap.postech.soat16.fase1.model.Customer;
import br.com.fiap.postech.soat16.fase1.model.Vehicle;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface VehicleMapper {

    default VehicleResponseDto toResponse(Vehicle entity) {
        if (entity == null) return null;
        return new VehicleResponseDto(
                entity.getId(),
                entity.getLicensePlate(),
                entity.getManufacturer(),
                entity.getModel(),
                entity.getColor(),
                entity.getYear(),
                entity.getKmDriven(),
                entity.getType(),
                null
                //entity.getCustomer().getCustomerId()
        );
    }

    default Vehicle toEntity(VehicleDto dto, Customer customer) {
        if (dto == null) return null;
        var entity = new Vehicle();
        entity.setLicensePlate(dto.licensePlate());
        entity.setManufacturer(dto.manufacturer());
        entity.setModel(dto.model());
        entity.setColor(dto.color());
        entity.setYear(dto.year());
        entity.setKmDriven(dto.kmDriven());
        entity.setType(dto.type());
        //entity.setCustomer(customer);
        return entity;
    }

    default void updateEntity(Vehicle vehicle, VehicleDto dto) {
        vehicle.setLicensePlate(dto.licensePlate());
        vehicle.setManufacturer(dto.manufacturer());
        vehicle.setModel(dto.model());
        vehicle.setColor(dto.color());
        vehicle.setYear(dto.year());
        vehicle.setKmDriven(dto.kmDriven());
        vehicle.setType(dto.type());
    }
}
