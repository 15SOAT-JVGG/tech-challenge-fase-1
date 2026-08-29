package br.com.fiap.postech.soat16.fase1.worker.application.port.out;

import java.util.List;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.worker.domain.model.Worker;

import io.smallrye.mutiny.Uni;

public interface WorkerPersistencePort {

    Uni<List<Worker>> findPage(int page, int size);

    Uni<Long> countWorkers();

    Uni<Worker> findByWorkerId(UUID id);

    Uni<Worker> findByEmail(String email);

    Uni<Boolean> existsByEmail(String email);

    Uni<Boolean> existsByEmailAndDifferentId(String email, UUID id);

    Uni<Worker> save(Worker worker);

    Uni<Long> deleteByWorkerId(UUID id);
}
