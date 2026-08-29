package br.com.fiap.postech.soat16.fase1.customer.adapter.out.vehicle;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.customer.application.port.out.CustomerVehicleLookupPort;
import br.com.fiap.postech.soat16.fase1.vehicle.application.port.out.VehiclePersistencePort;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class VehicleLookupAdapter implements CustomerVehicleLookupPort {

    private final VehiclePersistencePort vehiclePersistence;

    @Override
    public Uni<Boolean> existsByCustomerId(UUID customerId) {
        return vehiclePersistence.existsByCustomerId(customerId);
    }
}
