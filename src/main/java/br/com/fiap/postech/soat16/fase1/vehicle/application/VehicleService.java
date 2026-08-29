package br.com.fiap.postech.soat16.fase1.vehicle.application;

import static java.lang.Boolean.TRUE;

import java.util.Locale;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.customer.domain.exception.CustomerNotFoundException;
import br.com.fiap.postech.soat16.fase1.shared.application.result.PagedResult;
import br.com.fiap.postech.soat16.fase1.vehicle.application.command.CreateVehicleCommand;
import br.com.fiap.postech.soat16.fase1.vehicle.application.command.UpdateVehicleCommand;
import br.com.fiap.postech.soat16.fase1.vehicle.application.port.out.VehicleCustomerLookupPort;
import br.com.fiap.postech.soat16.fase1.vehicle.application.port.out.VehiclePersistencePort;
import br.com.fiap.postech.soat16.fase1.vehicle.application.query.VehicleQuery;
import br.com.fiap.postech.soat16.fase1.vehicle.application.result.VehicleResult;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.exception.DuplicateLicensePlateException;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.exception.VehicleNotFoundException;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.Vehicle;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleCustomerLookupPort customerLookup;
    private final VehiclePersistencePort vehiclePersistence;

    @WithSession
    public Uni<PagedResult<VehicleResult>> listAll(VehicleQuery query) {
        return Uni.combine().all()
                .unis(
                        vehiclePersistence.findPageWithFilter(query),
                        vehiclePersistence.countWithFilter(query))
                .asTuple()
                .map(tuple -> PagedResult.of(
                        tuple.getItem1().stream().map(VehicleResult::from).toList(),
                        query.page(),
                        query.size(),
                        tuple.getItem2()));
    }

    @WithSession
    public Uni<VehicleResult> findById(UUID id) {
        return vehiclePersistence.findByVehicleId(id)
                .onItem().ifNull().failWith(VehicleNotFoundException::new)
                .map(VehicleResult::from);
    }

    @WithSession
    public Uni<VehicleResult> findByLicensePlate(String licensePlate) {
        String normalizedLicensePlate = licensePlate.replace("-", "").toUpperCase(Locale.ROOT);
        return vehiclePersistence.findByLicensePlate(normalizedLicensePlate)
                .onItem().ifNull().failWith(() -> new VehicleNotFoundException(licensePlate))
                .map(VehicleResult::from);
    }

    @WithTransaction
    public Uni<Void> create(CreateVehicleCommand command) {
        return customerLookup.findByCustomerId(command.customerId())
                .onItem().ifNull().failWith(CustomerNotFoundException::new)
                .flatMap(customer -> vehiclePersistence.existsByLicensePlate(command.licensePlate())
                        .flatMap(exists -> {
                            if (TRUE.equals(exists)) {
                                return Uni.createFrom().<Void>failure(
                                        new DuplicateLicensePlateException());
                            }
                            Vehicle vehicle = Vehicle.create(
                                    customer,
                                    command.licensePlate(),
                                    command.manufacturer(),
                                    command.model(),
                                    command.color(),
                                    command.year(),
                                    command.kmDriven(),
                                    command.type());
                            return vehiclePersistence.save(vehicle).replaceWithVoid();
                        }));
    }

    @WithTransaction
    public Uni<VehicleResult> update(UpdateVehicleCommand command) {
        return vehiclePersistence.findByVehicleId(command.id())
                .onItem().ifNull().failWith(VehicleNotFoundException::new)
                .flatMap(vehicle -> {
                    vehicle.update(
                            command.licensePlate(),
                            command.manufacturer(),
                            command.model(),
                            command.color(),
                            command.year(),
                            command.kmDriven(),
                            command.type());
                    return vehiclePersistence.save(vehicle);
                })
                .map(VehicleResult::from);
    }

    @WithTransaction
    public Uni<Void> delete(UUID id) {
        return vehiclePersistence.deleteByVehicleId(id)
                .flatMap(deleted -> {
                    if (deleted == 0) {
                        return Uni.createFrom().<Void>failure(new VehicleNotFoundException());
                    }
                    return Uni.createFrom().voidItem();
                });
    }
}
