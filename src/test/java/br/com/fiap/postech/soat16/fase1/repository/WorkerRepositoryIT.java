package br.com.fiap.postech.soat16.fase1.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.model.Worker;
import br.com.fiap.postech.soat16.fase1.model.enums.WorkerProfile;
import br.com.fiap.postech.soat16.fase1.security.PostgresTestResource;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Diferente dos demais repositórios (Hibernate Reactive Panache), {@link WorkerRepository} usa
 * Hibernate ORM Panache (blocking) — daí o uso de {@code @TestTransaction} em vez do helper
 * reativo (VertxContextSupport/Panache.withTransaction) usado nos outros *RepositoryIT.
 * {@code @TestTransaction} é uma interceptor binding do Arc/CDI e não pode ser aplicada a métodos
 * de classes {@code @Nested} (proibido pelas regras de CDI) — por isso os testes ficam no nível
 * da classe, sem agrupamento em {@code @Nested}. Cada teste roda na sua própria transação,
 * revertida automaticamente ao final.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@DisplayName("WorkerRepository — Integration Tests")
class WorkerRepositoryIT {

    @Inject
    WorkerRepository repository;

    private Worker seed(String email) {
        Worker worker = new Worker();
        worker.setProfile(WorkerProfile.MECHANIC);
        worker.setFirstName("Joao");
        worker.setLastName("Pereira");
        worker.setEmail(email);
        worker.setPhoneNumber("+5511988887777");
        worker.setPasswordHash("hashed");
        worker.setActive(true);
        repository.persist(worker);
        return worker;
    }

    private static String uniqueEmail() {
        return "worker-" + UUID.randomUUID() + "@oficina.com";
    }

    @Test
    @TestTransaction
    @DisplayName("findByEmail finds a persisted worker by exact email")
    void findsByEmail() {
        String email = uniqueEmail();
        seed(email);

        Optional<Worker> found = repository.findByEmail(email);

        assertTrue(found.isPresent());
        assertEquals(email, found.get().getEmail());
    }

    @Test
    @TestTransaction
    @DisplayName("findByEmail returns empty when no worker has the given email")
    void returnsEmptyWhenNotFound() {
        assertTrue(repository.findByEmail(uniqueEmail()).isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("existsByEmail distinguishes a persisted email from an unknown one")
    void existsByEmail() {
        String email = uniqueEmail();
        seed(email);

        assertTrue(repository.existsByEmail(email));
        assertFalse(repository.existsByEmail(uniqueEmail()));
    }

    @Test
    @TestTransaction
    @DisplayName("existsByEmailAndDifferentId is true when another worker already owns the email")
    void trueWhenAnotherWorkerOwnsEmail() {
        String email = uniqueEmail();
        Worker existing = seed(email);

        boolean clashesWithSomeoneElse = repository.existsByEmailAndDifferentId(email, UUID.randomUUID());

        assertTrue(clashesWithSomeoneElse);
        // sanity-check: the seeded worker is indeed the one holding the email
        assertEquals(email, existing.getEmail());
    }

    @Test
    @TestTransaction
    @DisplayName("existsByEmailAndDifferentId is false when the email belongs to the same worker being checked")
    void falseWhenEmailBelongsToSameWorker() {
        Worker worker = seed(uniqueEmail());

        assertFalse(repository.existsByEmailAndDifferentId(worker.getEmail(), worker.getId()));
    }

    @Test
    @TestTransaction
    @DisplayName("findByWorkerId finds a persisted worker by id")
    void findsById() {
        Worker worker = seed(uniqueEmail());

        Optional<Worker> found = repository.findByWorkerId(worker.getId());

        assertTrue(found.isPresent());
    }

    @Test
    @TestTransaction
    @DisplayName("findByWorkerId returns empty when the id does not exist")
    void returnsEmptyWhenIdNotFound() {
        assertTrue(repository.findByWorkerId(UUID.randomUUID()).isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("deleteByWorkerId deletes the worker and the count reflects the removal")
    void deletesById() {
        Worker worker = seed(uniqueEmail());

        long deleted = repository.deleteByWorkerId(worker.getId());

        assertEquals(1L, deleted);
        assertTrue(repository.findByWorkerId(worker.getId()).isEmpty());
    }

    @Test
    @TestTransaction
    @DisplayName("findPage returns persisted workers")
    void returnsPersistedWorkers() {
        seed(uniqueEmail());
        seed(uniqueEmail());

        List<Worker> page = repository.findPage(0, 10);

        assertTrue(page.size() >= 2);
    }

    @Test
    @TestTransaction
    @DisplayName("findPage returns an empty list when the requested page is past the last result")
    void returnsEmptyListWhenPageOutOfRange() {
        seed(uniqueEmail());

        List<Worker> page = repository.findPage(1000, 10);

        assertTrue(page.isEmpty());
    }
}
