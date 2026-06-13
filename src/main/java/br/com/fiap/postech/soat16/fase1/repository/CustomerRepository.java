package br.com.fiap.postech.soat16.fase1.repository;

import br.com.fiap.postech.soat16.fase1.model.Customer;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

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

    public Uni<Customer> findByDocument(String document) {
        return find("document = ?1", document).firstResult();
    }

    public Uni<Boolean> existsByDocument(String document) {
        return count("document = ?1", document)
                .map(total -> total > 0);
    }
}
