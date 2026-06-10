package br.com.fiap.postech.soat16.fase1.repository;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.model.Customer;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class CustomerRepository implements PanacheRepository<Customer> {

    public Uni<List<Customer>> findPage(int page, int size) {
        return find("ORDER BY createdAt DESC").page(page, size).list();
    }

    public Uni<Customer> findByCustomerId(UUID id) {
        return find("customerId = ?1", id).firstResult()
                .invoke(found -> Log.infof("Customer lookup: id=%s found=%b", id, found != null));
    }

    public Uni<Long> deleteByCustomerId(UUID id) {
        return delete("customerId = ?1", id)
                .invoke(deleted -> Log.infof("Customer deleted: id=%s deleted=%d", id, deleted));
    }

    public Uni<Boolean> existsByPhoneNumber(String phoneNumber) {
        return count("phoneNumber = ?1", phoneNumber)
                .map(total -> total > 0);
    }
}
