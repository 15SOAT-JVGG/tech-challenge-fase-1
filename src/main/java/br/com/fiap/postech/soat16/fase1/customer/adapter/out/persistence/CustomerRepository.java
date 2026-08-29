package br.com.fiap.postech.soat16.fase1.customer.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.customer.application.port.out.CustomerPersistencePort;
import br.com.fiap.postech.soat16.fase1.customer.domain.model.Customer;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class CustomerRepository
        implements PanacheRepositoryBase<Customer, UUID>, CustomerPersistencePort {

    @Override
    public Uni<List<Customer>> findPage(int page, int size) {
        return find("ORDER BY createdAt DESC").page(page, size).list();
    }

    @Override
    public Uni<Long> countCustomers() {
        return count();
    }

    @Override
    public Uni<Customer> findByCustomerId(UUID id) {
        return findById(id)
                .invoke(found -> Log.infof("Customer lookup: id=%s found=%b", id, found != null));
    }

    @Override
    public Uni<Long> deleteByCustomerId(UUID id) {
        return delete("id = ?1", id)
                .invoke(deleted -> Log.infof("Customer deleted: id=%s deleted=%d", id, deleted));
    }

    @Override
    public Uni<Customer> findByDocument(String document) {
        return find("document = ?1", document).firstResult();
    }

    @Override
    public Uni<Boolean> existsByDocument(String document) {
        return count("document = ?1", document).map(total -> total > 0);
    }

    @Override
    public Uni<Customer> save(Customer customer) {
        return persist(customer);
    }
}
