package br.com.fiap.postech.soat16.fase1.repository;

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

import br.com.fiap.postech.soat16.fase1.model.Worker;
import br.com.fiap.postech.soat16.fase1.model.enums.WorkerProfile;
import br.com.fiap.postech.soat16.fase1.security.PostgresTestResource;

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

    private <T> T inTransaction(Supplier<Uni<T>> action) {
        try {
            return VertxContextSupport.subscribeAndAwait(() -> Panache.withTransaction(action));
        } catch (Throwable e) {
            throw new IllegalStateException("Falha ao preparar dados de teste", e);
        }
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
        return inTransaction(() -> repository.persist(worker));
    }

    private static String uniqueEmail() {
        return "worker-" + UUID.randomUUID() + "@oficina.com";
    }

    @Test
    @DisplayName("findByEmail finds a persisted worker by exact email")
    void findsByEmail() {
        String email = uniqueEmail();
        seed(email);

        Worker found = inTransaction(() -> repository.findByEmail(email));

        assertEquals(email, found.getEmail());
    }

    @Test
    @DisplayName("findByEmail returns null when no worker has the given email")
    void returnsNullWhenNotFound() {
        Worker found = inTransaction(() -> repository.findByEmail(uniqueEmail()));

        assertNull(found);
    }

    @Test
    @DisplayName("existsByEmail distinguishes a persisted email from an unknown one")
    void existsByEmail() {
        String email = uniqueEmail();
        seed(email);

        assertTrue(inTransaction(() -> repository.existsByEmail(email)));
        assertFalse(inTransaction(() -> repository.existsByEmail(uniqueEmail())));
    }

    @Test
    @DisplayName("existsByEmailAndDifferentId is true when another worker already owns the email")
    void trueWhenAnotherWorkerOwnsEmail() {
        String email = uniqueEmail();
        Worker existing = seed(email);

        boolean clashesWithSomeoneElse = inTransaction(
                () -> repository.existsByEmailAndDifferentId(email, UUID.randomUUID()));

        assertTrue(clashesWithSomeoneElse);
        assertEquals(email, existing.getEmail());
    }

    @Test
    @DisplayName("existsByEmailAndDifferentId is false when the email belongs to the same worker being checked")
    void falseWhenEmailBelongsToSameWorker() {
        Worker worker = seed(uniqueEmail());

        assertFalse(inTransaction(
                () -> repository.existsByEmailAndDifferentId(worker.getEmail(), worker.getId())));
    }

    @Test
    @DisplayName("findByWorkerId finds a persisted worker by id")
    void findsById() {
        Worker worker = seed(uniqueEmail());

        Worker found = inTransaction(() -> repository.findByWorkerId(worker.getId()));

        assertEquals(worker.getId(), found.getId());
    }

    @Test
    @DisplayName("findByWorkerId returns null when the id does not exist")
    void returnsNullWhenIdNotFound() {
        Worker found = inTransaction(() -> repository.findByWorkerId(UUID.randomUUID()));

        assertNull(found);
    }

    @Test
    @DisplayName("deleteByWorkerId deletes the worker and the count reflects the removal")
    void deletesById() {
        Worker worker = seed(uniqueEmail());

        Long deleted = inTransaction(() -> repository.deleteByWorkerId(worker.getId()));

        assertEquals(1L, deleted);
        assertNull(inTransaction(() -> repository.findByWorkerId(worker.getId())));
    }

    @Test
    @DisplayName("findPage returns persisted workers")
    void returnsPersistedWorkers() {
        seed(uniqueEmail());
        seed(uniqueEmail());

        List<Worker> page = inTransaction(() -> repository.findPage(0, 10));

        assertTrue(page.size() >= 2);
    }

    @Test
    @DisplayName("findPage returns an empty list when the requested page is past the last result")
    void returnsEmptyListWhenPageOutOfRange() {
        seed(uniqueEmail());

        List<Worker> page = inTransaction(() -> repository.findPage(1000, 10));

        assertTrue(page.isEmpty());
    }
}
