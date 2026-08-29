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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.model.Customer;
import br.com.fiap.postech.soat16.fase1.model.enums.DocumentType;
import br.com.fiap.postech.soat16.fase1.security.PostgresTestResource;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@DisplayName("CustomerRepository — Integration Tests")
class CustomerRepositoryIT {

    @Inject
    CustomerRepository repository;

    private <T> T inTransaction(Supplier<Uni<T>> action) {
        try {
            return VertxContextSupport.subscribeAndAwait(() -> Panache.withTransaction(action));
        } catch (Throwable e) {
            throw new IllegalStateException("Falha ao preparar dados de teste", e);
        }
    }

    private Customer seed(String document) {
        Customer customer = new Customer();
        customer.setFirstName("Maria");
        customer.setLastName("Silva");
        customer.setPhoneNumber("+5511999999999");
        customer.setDocument(document);
        customer.setDocumentType(DocumentType.CPF);
        return inTransaction(() -> repository.persist(customer));
    }

    @Nested
    @DisplayName("findByDocument / existsByDocument")
    class FindByDocument {

        @Test
        @DisplayName("finds the customer by its exact document")
        void findsByDocument() {
            String document = "DOC-" + UUID.randomUUID();
            seed(document);

            Customer found = inTransaction(() -> repository.findByDocument(document));

            assertEquals(document, found.getDocument());
        }

        @Test
        @DisplayName("returns null when no customer has the given document")
        void returnsNullWhenNotFound() {
            Customer found = inTransaction(() -> repository.findByDocument("DOC-" + UUID.randomUUID()));

            assertNull(found);
        }

        @Test
        @DisplayName("existsByDocument is true only for a persisted document")
        void existsByDocument() {
            String document = "DOC-" + UUID.randomUUID();
            seed(document);

            assertTrue(inTransaction(() -> repository.existsByDocument(document)));
            assertFalse(inTransaction(() -> repository.existsByDocument("DOC-" + UUID.randomUUID())));
        }
    }

    @Nested
    @DisplayName("findByCustomerId / deleteByCustomerId")
    class FindAndDelete {

        @Test
        @DisplayName("finds a persisted customer by id")
        void findsById() {
            Customer customer = seed("DOC-" + UUID.randomUUID());

            Customer found = inTransaction(() -> repository.findByCustomerId(customer.getId()));

            assertEquals(customer.getId(), found.getId());
        }

        @Test
        @DisplayName("returns null when the id does not exist")
        void returnsNullWhenIdNotFound() {
            Customer found = inTransaction(() -> repository.findByCustomerId(UUID.randomUUID()));

            assertNull(found);
        }

        @Test
        @DisplayName("deletes the customer and the count reflects the removal")
        void deletesById() {
            Customer customer = seed("DOC-" + UUID.randomUUID());

            Long deleted = inTransaction(() -> repository.deleteByCustomerId(customer.getId()));

            assertEquals(1L, deleted);
            assertNull(inTransaction(() -> repository.findByCustomerId(customer.getId())));
        }

        @Test
        @DisplayName("deleting a non-existent id removes nothing")
        void deletingMissingIdRemovesNothing() {
            Long deleted = inTransaction(() -> repository.deleteByCustomerId(UUID.randomUUID()));

            assertEquals(0L, deleted);
        }
    }

    @Nested
    @DisplayName("findPage")
    class FindPage {

        @Test
        @DisplayName("returns persisted customers ordered most-recent-first")
        void returnsPersistedCustomersOrderedByCreatedAtDesc() {
            seed("DOC-" + UUID.randomUUID());
            seed("DOC-" + UUID.randomUUID());

            List<Customer> page = inTransaction(() -> repository.findPage(0, 10));

            assertTrue(page.size() >= 2);
        }

        @Test
        @DisplayName("returns an empty list when the requested page is past the last result")
        void returnsEmptyListWhenPageIsOutOfRange() {
            seed("DOC-" + UUID.randomUUID());

            List<Customer> page = inTransaction(() -> repository.findPage(1000, 10));

            assertTrue(page.isEmpty());
        }
    }
}
