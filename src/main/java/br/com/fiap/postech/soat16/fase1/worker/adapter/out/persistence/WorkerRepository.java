package br.com.fiap.postech.soat16.fase1.worker.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.worker.adapter.out.persistence.entity.WorkerJpaEntity;
import br.com.fiap.postech.soat16.fase1.worker.adapter.out.persistence.mapper.WorkerPersistenceMapper;
import br.com.fiap.postech.soat16.fase1.worker.application.port.out.WorkerPersistencePort;
import br.com.fiap.postech.soat16.fase1.worker.domain.model.Worker;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class WorkerRepository
        implements PanacheRepositoryBase<WorkerJpaEntity, UUID>, WorkerPersistencePort {

    @Override
    public Uni<List<Worker>> findPage(int page, int size) {
        return find("ORDER BY createdAt DESC").page(Page.of(page, size)).list()
                .map(entities -> entities.stream()
                        .map(WorkerPersistenceMapper::toDomain)
                        .toList());
    }

    @Override
    public Uni<Long> countWorkers() {
        return count();
    }

    @Override
    public Uni<Worker> findByWorkerId(UUID id) {
        return findById(id)
                .invoke(found -> Log.infof("Worker lookup: id=%s found=%b", id, found != null))
                .map(WorkerPersistenceMapper::toDomain);
    }

    @Override
    public Uni<Worker> findByEmail(String email) {
        return find("email = ?1", email).firstResult()
                .map(WorkerPersistenceMapper::toDomain);
    }

    @Override
    public Uni<Long> deleteByWorkerId(UUID id) {
        return delete("id = ?1", id)
                .invoke(deleted -> Log.infof("Worker deleted: id=%s deleted=%d", id, deleted));
    }

    @Override
    public Uni<Boolean> existsByEmail(String email) {
        return count("email = ?1", email).map(total -> total > 0);
    }

    @Override
    public Uni<Boolean> existsByEmailAndDifferentId(String email, UUID id) {
        return count("email = ?1 and id <> ?2", email, id).map(total -> total > 0);
    }

    @Override
    public Uni<Worker> save(Worker worker) {
        if (worker.getId() == null) {
            return persist(WorkerPersistenceMapper.toJpaEntity(worker))
                    .map(WorkerPersistenceMapper::toDomain);
        }
        return findById(worker.getId())
                .onItem().ifNotNull().transform(entity -> {
                    WorkerPersistenceMapper.copyState(worker, entity);
                    return WorkerPersistenceMapper.toDomain(entity);
                });
    }
}
