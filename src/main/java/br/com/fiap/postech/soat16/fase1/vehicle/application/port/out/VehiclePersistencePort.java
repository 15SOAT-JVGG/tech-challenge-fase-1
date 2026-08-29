package br.com.fiap.postech.soat16.fase1.vehicle.application.port.out;

import java.util.List;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.vehicle.application.query.VehicleQuery;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.Vehicle;

import io.smallrye.mutiny.Uni;

public interface VehiclePersistencePort {

    Uni<List<Vehicle>> findPageWithFilter(VehicleQuery query);

    Uni<Long> countWithFilter(VehicleQuery query);

    Uni<Vehicle> findByVehicleId(UUID id);

    Uni<Vehicle> findByLicensePlate(String licensePlate);

    Uni<Boolean> existsByLicensePlate(String licensePlate);

    Uni<Boolean> existsByCustomerId(UUID customerId);

    Uni<Vehicle> save(Vehicle vehicle);

    Uni<Long> deleteByVehicleId(UUID id);
}
