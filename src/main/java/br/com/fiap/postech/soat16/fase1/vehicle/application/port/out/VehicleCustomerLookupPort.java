package br.com.fiap.postech.soat16.fase1.vehicle.application.port.out;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.customer.domain.model.Customer;

import io.smallrye.mutiny.Uni;

@FunctionalInterface
public interface VehicleCustomerLookupPort {

    Uni<Customer> findByCustomerId(UUID customerId);
}
