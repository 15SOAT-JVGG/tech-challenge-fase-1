package br.com.fiap.postech.soat16.fase1.vehicle.adapter.out.customer;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.customer.application.port.out.CustomerPersistencePort;
import br.com.fiap.postech.soat16.fase1.customer.domain.model.Customer;
import br.com.fiap.postech.soat16.fase1.vehicle.application.port.out.VehicleCustomerLookupPort;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class CustomerLookupAdapter implements VehicleCustomerLookupPort {

    private final CustomerPersistencePort customerPersistence;

    @Override
    public Uni<Customer> findByCustomerId(UUID customerId) {
        return customerPersistence.findByCustomerId(customerId);
    }
}
