package br.com.fiap.postech.soat16.fase1.repository;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.model.Worker;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class WorkerRepository implements PanacheRepository<Worker> {

    public Uni<List<Worker>> findPage(int page, int size) {
        return find("ORDER BY createdAt DESC").page(Page.of(page, size)).list();
    }

    public Uni<Worker> findByWorkerId(UUID id) {
        return find("id = ?1", id).firstResult()
                .invoke(found -> Log.infof("Worker lookup: id=%s found=%b", id, found != null));
    }

    public Uni<Worker> findByEmail(String email) {
        return find("email = ?1", email).firstResult();
    }

    public Uni<Long> deleteByWorkerId(UUID id) {
        return delete("id = ?1", id)
                .invoke(deleted -> Log.infof("Worker deleted: id=%s deleted=%d", id, deleted));
    }

    public Uni<Boolean> existsByEmail(String email) {
        return count("email = ?1", email).map(total -> total > 0);
    }

    public Uni<Boolean> existsByEmailAndDifferentId(String email, UUID id) {
        return count("email = ?1 and id <> ?2", email, id).map(total -> total > 0);
    }
}
