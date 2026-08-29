package br.com.fiap.postech.soat16.fase1.vehicle.adapter.out.persistence.mapper;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.customer.adapter.out.persistence.mapper.CustomerPersistenceMapper;
import br.com.fiap.postech.soat16.fase1.shared.adapter.out.persistence.AuditPersistenceMapper;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.out.persistence.entity.VehicleJpaEntity;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.Vehicle;

public final class VehiclePersistenceMapper {

    private VehiclePersistenceMapper() {
    }

    public static Vehicle toDomain(VehicleJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        var vehicle = new Vehicle();
        vehicle.setId(entity.getId());
        vehicle.setCustomer(CustomerPersistenceMapper.toDomain(entity.getCustomer()));
        vehicle.setLicensePlate(entity.getLicensePlate());
        vehicle.setManufacturer(entity.getManufacturer());
        vehicle.setModel(entity.getModel());
        vehicle.setColor(entity.getColor());
        vehicle.setYear(entity.getYear());
        vehicle.setKmDriven(entity.getKmDriven());
        vehicle.setType(entity.getType());
        AuditPersistenceMapper.copyToDomain(entity, vehicle);
        return vehicle;
    }

    public static VehicleJpaEntity toJpaEntity(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }
        var entity = new VehicleJpaEntity();
        entity.setId(vehicle.getId());
        entity.setCustomer(CustomerPersistenceMapper.toJpaReference(
                vehicle.getCustomer() != null ? vehicle.getCustomer().getId() : null));
        copyState(vehicle, entity);
        AuditPersistenceMapper.copyToJpaEntity(vehicle, entity);
        return entity;
    }

    /**
     * Referência somente com identidade, usada para preencher chaves estrangeiras sem carregar o
     * agregado completo de outro contexto.
     */
    public static VehicleJpaEntity toJpaReference(UUID vehicleId) {
        if (vehicleId == null) {
            return null;
        }
        var entity = new VehicleJpaEntity();
        entity.setId(vehicleId);
        return entity;
    }

    public static void copyState(Vehicle source, VehicleJpaEntity target) {
        target.setLicensePlate(source.getLicensePlate());
        target.setManufacturer(source.getManufacturer());
        target.setModel(source.getModel());
        target.setColor(source.getColor());
        target.setYear(source.getYear());
        target.setKmDriven(source.getKmDriven());
        target.setType(source.getType());
    }

    public static void copyGeneratedState(VehicleJpaEntity source, Vehicle target) {
        target.setId(source.getId());
        AuditPersistenceMapper.copyToDomain(source, target);
    }
}
