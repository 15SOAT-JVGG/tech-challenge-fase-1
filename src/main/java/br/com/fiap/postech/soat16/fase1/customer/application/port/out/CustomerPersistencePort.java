package br.com.fiap.postech.soat16.fase1.customer.application.port.out;

import java.util.List;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.customer.domain.model.Customer;

import io.smallrye.mutiny.Uni;

public interface CustomerPersistencePort {

    Uni<List<Customer>> findPage(int page, int size);

    Uni<Long> countCustomers();

    Uni<Customer> findByCustomerId(UUID id);

    Uni<Customer> findByDocument(String document);

    Uni<Boolean> existsByDocument(String document);

    Uni<Customer> save(Customer customer);

    Uni<Long> deleteByCustomerId(UUID id);
}
