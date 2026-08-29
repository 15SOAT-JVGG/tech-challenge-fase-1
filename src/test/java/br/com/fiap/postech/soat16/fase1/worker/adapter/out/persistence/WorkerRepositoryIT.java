package br.com.fiap.postech.soat16.fase1.worker.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.shared.test.infrastructure.PostgresTestResource;
import br.com.fiap.postech.soat16.fase1.worker.domain.model.Worker;
import br.com.fiap.postech.soat16.fase1.worker.domain.model.enums.WorkerProfile;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@DisplayName("WorkerRepository — Integration Tests")
class WorkerRepositoryIT {

    @Inject
    WorkerRepository repository;

    @Test
    @DisplayName("finds a worker by exact email")
    void findsByEmail() {
        String email = uniqueEmail();
        seed(email);

        Worker found = inTransaction(() -> repository.findByEmail(email));

        assertEquals(email, found.getEmail());
    }

    @Test
    @DisplayName("returns null when no worker has the email")
    void returnsNullWhenEmailIsMissing() {
        assertNull(inTransaction(() -> repository.findByEmail(uniqueEmail())));
    }

    @Test
    @DisplayName("checks email existence")
    void checksEmailExistence() {
        String email = uniqueEmail();
        seed(email);

        assertTrue(inTransaction(() -> repository.existsByEmail(email)));
        assertFalse(inTransaction(() -> repository.existsByEmail(uniqueEmail())));
    }

    @Test
    @DisplayName("detects an email owned by another worker")
    void detectsEmailOwnedByAnotherWorker() {
        String email = uniqueEmail();
        seed(email);

        assertTrue(inTransaction(
                () -> repository.existsByEmailAndDifferentId(email, UUID.randomUUID())));
    }

    @Test
    @DisplayName("accepts the email owned by the worker being updated")
    void acceptsSameWorkerEmail() {
        Worker worker = seed(uniqueEmail());

        assertFalse(inTransaction(
                () -> repository.existsByEmailAndDifferentId(worker.getEmail(), worker.getId())));
    }

    @Test
    @DisplayName("finds a worker by id")
    void findsById() {
        Worker worker = seed(uniqueEmail());

        Worker found = inTransaction(() -> repository.findByWorkerId(worker.getId()));

        assertEquals(worker.getId(), found.getId());
    }

    @Test
    @DisplayName("returns null when no worker has the id")
    void returnsNullWhenIdIsMissing() {
        assertNull(inTransaction(() -> repository.findByWorkerId(UUID.randomUUID())));
    }

    @Test
    @DisplayName("deletes a worker by id")
    void deletesById() {
        Worker worker = seed(uniqueEmail());

        Long deleted = inTransaction(() -> repository.deleteByWorkerId(worker.getId()));

        assertEquals(1L, deleted);
        assertNull(inTransaction(() -> repository.findByWorkerId(worker.getId())));
    }

    @Test
    @DisplayName("pages workers and exposes their total count")
    void pagesWorkers() {
        Worker first = seed(uniqueEmail());
        seed(uniqueEmail());

        List<Worker> page = inTransaction(() -> repository.findPage(0, 100));
        Long count = inTransaction(repository::countWorkers);

        assertTrue(page.stream().anyMatch(item -> item.getId().equals(first.getId())));
        assertTrue(count >= 2);
    }

    @Test
    @DisplayName("returns an empty list for a page beyond the result set")
    void returnsEmptyPage() {
        seed(uniqueEmail());

        List<Worker> page = inTransaction(() -> repository.findPage(1000, 10));

        assertTrue(page.isEmpty());
    }

    private Worker seed(String email) {
        Worker worker = new Worker();
        worker.setProfile(WorkerProfile.MECHANIC);
        worker.setFirstName("Joao");
        worker.setLastName("Pereira");
        worker.setEmail(email);
        worker.setPhoneNumber("+5511988887777");
        worker.setPasswordHash("hashed");
        worker.setActive(true);
        return inTransaction(() -> repository.save(worker));
    }

    private static String uniqueEmail() {
        return "worker-" + UUID.randomUUID() + "@oficina.com";
    }

    private <T> T inTransaction(Supplier<Uni<T>> action) {
        try {
            return VertxContextSupport.subscribeAndAwait(() -> Panache.withTransaction(action));
        } catch (Throwable exception) {
            throw new IllegalStateException("Falha ao preparar dados de teste", exception);
        }
    }
}
