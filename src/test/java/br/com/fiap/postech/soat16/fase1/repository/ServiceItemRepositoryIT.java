package br.com.fiap.postech.soat16.fase1.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.model.ServiceItem;
import br.com.fiap.postech.soat16.fase1.security.PostgresTestResource;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;

/**
 * {@link ServiceItemRepository} não declara consultas customizadas, então estes testes cobrem o
 * CRUD herdado de {@code PanacheRepositoryBase} (persist/findById/deleteById) sobre um banco real,
 * seguindo o mesmo padrão dos demais *RepositoryIT.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@DisplayName("ServiceItemRepository — Integration Tests")
class ServiceItemRepositoryIT {

    @Inject
    ServiceItemRepository repository;

    private <T> T inTransaction(Supplier<Uni<T>> action) {
        try {
            return VertxContextSupport.subscribeAndAwait(() -> Panache.withTransaction(action));
        } catch (Throwable e) {
            throw new IllegalStateException("Falha ao preparar dados de teste", e);
        }
    }

    private ServiceItem seed(String name) {
        ServiceItem serviceItem = new ServiceItem();
        serviceItem.setName(name);
        serviceItem.setDescription("desc");
        serviceItem.setBasePrice(new BigDecimal("80.00"));
        serviceItem.setEstimatedDurationMinutes(30);
        serviceItem.setActive(true);
        return inTransaction(() -> repository.persist(serviceItem));
    }

    @Nested
    @DisplayName("findById / persist")
    class FindByIdAndPersist {

        @Test
        @DisplayName("finds a persisted service item by id")
        void findsById() {
            ServiceItem serviceItem = seed("Alinhamento - " + UUID.randomUUID());

            ServiceItem found = inTransaction(() -> repository.findById(serviceItem.getId()));

            assertEquals(serviceItem.getId(), found.getId());
            assertEquals(serviceItem.getName(), found.getName());
        }

        @Test
        @DisplayName("returns null when the id does not exist")
        void returnsNullWhenNotFound() {
            assertNull(inTransaction(() -> repository.findById(UUID.randomUUID())));
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        @Test
        @DisplayName("deletes the service item and a subsequent lookup returns null")
        void deletesById() {
            ServiceItem serviceItem = seed("Balanceamento - " + UUID.randomUUID());

            Boolean deleted = inTransaction(() -> repository.deleteById(serviceItem.getId()));

            assertTrue(deleted);
            assertNull(inTransaction(() -> repository.findById(serviceItem.getId())));
        }

        @Test
        @DisplayName("deleting a non-existent id returns false")
        void deletingMissingIdReturnsFalse() {
            Boolean deleted = inTransaction(() -> repository.deleteById(UUID.randomUUID()));

            assertEquals(Boolean.FALSE, deleted);
        }
    }
}
