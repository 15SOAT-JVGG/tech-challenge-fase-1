package br.com.fiap.postech.soat16.fase1.customer.application.port.out;

import java.util.UUID;

import io.smallrye.mutiny.Uni;

@FunctionalInterface
public interface CustomerVehicleLookupPort {

    Uni<Boolean> existsByCustomerId(UUID customerId);
}
