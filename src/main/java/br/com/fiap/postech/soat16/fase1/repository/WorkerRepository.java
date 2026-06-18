package br.com.fiap.postech.soat16.fase1.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.model.Worker;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;

@ApplicationScoped
public class WorkerRepository implements PanacheRepository<Worker> {

    public List<Worker> findPage(int page, int size) {
        return find("ORDER BY createdAt DESC").page(Page.of(page, size)).list();
    }

    public Optional<Worker> findByWorkerId(UUID id) {
        Optional<Worker> found = find("id = ?1", id).firstResultOptional();
        Log.infof("Worker lookup: id=%s found=%b", id, found.isPresent());
        return found;
    }

    public Optional<Worker> findByEmail(String email) {
        return find("email = ?1", email).firstResultOptional();
    }

    public long deleteByWorkerId(UUID id) {
        long deleted = delete("id = ?1", id);
        Log.infof("Worker deleted: id=%s deleted=%d", id, deleted);
        return deleted;
    }

    public boolean existsByEmail(String email) {
        return count("email = ?1", email) > 0;
    }

    public boolean existsByEmailAndDifferentId(String email, UUID id) {
        return count("email = ?1 and id <> ?2", email, id) > 0;
    }
}
