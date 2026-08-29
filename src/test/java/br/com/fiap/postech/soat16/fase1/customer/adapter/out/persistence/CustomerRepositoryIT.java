package br.com.fiap.postech.soat16.fase1.customer.adapter.out.persistence;

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

import br.com.fiap.postech.soat16.fase1.customer.domain.model.Customer;
import br.com.fiap.postech.soat16.fase1.customer.domain.model.enums.DocumentType;
import br.com.fiap.postech.soat16.fase1.shared.test.infrastructure.PostgresTestResource;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@DisplayName("CustomerRepository — Testes de integração")
class CustomerRepositoryIT {

    @Inject
    CustomerRepository repository;

    @Nested
    @DisplayName("findByDocument / existsByDocument")
    class FindByDocument {

        @Test
        @DisplayName("deve encontrar o cliente pelo documento exato")
        void shouldFindCustomerByExactDocument() {
            String document = "DOC-" + UUID.randomUUID();
            seed(document);

            Customer found = inTransaction(() -> repository.findByDocument(document));

            assertEquals(document, found.getDocument());
        }

        @Test
        @DisplayName("deve retornar nulo quando o documento não existir")
        void shouldReturnNullWhenDocumentDoesNotExist() {
            Customer found = inTransaction(
                    () -> repository.findByDocument("DOC-" + UUID.randomUUID()));

            assertNull(found);
        }

        @Test
        @DisplayName("deve identificar somente documentos persistidos")
        void shouldIdentifyOnlyPersistedDocuments() {
            String document = "DOC-" + UUID.randomUUID();
            seed(document);

            assertTrue(inTransaction(() -> repository.existsByDocument(document)));
            assertFalse(inTransaction(
                    () -> repository.existsByDocument("DOC-" + UUID.randomUUID())));
        }
    }

    @Nested
    @DisplayName("findByCustomerId / deleteByCustomerId")
    class FindAndDelete {

        @Test
        @DisplayName("deve encontrar um cliente persistido pelo identificador")
        void shouldFindPersistedCustomerById() {
            Customer customer = seed("DOC-" + UUID.randomUUID());

            Customer found = inTransaction(
                    () -> repository.findByCustomerId(customer.getId()));

            assertEquals(customer.getId(), found.getId());
        }

        @Test
        @DisplayName("deve retornar nulo quando o identificador não existir")
        void shouldReturnNullWhenIdDoesNotExist() {
            Customer found = inTransaction(
                    () -> repository.findByCustomerId(UUID.randomUUID()));

            assertNull(found);
        }

        @Test
        @DisplayName("deve excluir o cliente e atualizar a contagem")
        void shouldDeleteCustomerAndUpdateCount() {
            long countBefore = inTransaction(repository::countCustomers);
            Customer customer = seed("DOC-" + UUID.randomUUID());

            assertEquals(countBefore + 1, inTransaction(repository::countCustomers));

            Long deleted = inTransaction(
                    () -> repository.deleteByCustomerId(customer.getId()));

            assertEquals(1L, deleted);
            assertNull(inTransaction(
                    () -> repository.findByCustomerId(customer.getId())));
            assertEquals(countBefore, inTransaction(repository::countCustomers));
        }

        @Test
        @DisplayName("deve retornar zero ao excluir um identificador inexistente")
        void shouldReturnZeroWhenDeletingMissingId() {
            Long deleted = inTransaction(
                    () -> repository.deleteByCustomerId(UUID.randomUUID()));

            assertEquals(0L, deleted);
        }
    }

    @Test
    @DisplayName("deve contar os clientes persistidos")
    void shouldCountPersistedCustomers() {
        long countBefore = inTransaction(repository::countCustomers);

        seed("DOC-" + UUID.randomUUID());

        assertEquals(countBefore + 1, inTransaction(repository::countCustomers));
    }

    @Nested
    @DisplayName("findPage")
    class FindPage {

        @Test
        @DisplayName("deve retornar os clientes do mais recente para o mais antigo")
        void shouldReturnCustomersNewestFirst() {
            Customer older = seed("DOC-" + UUID.randomUUID());
            Customer newer = seed("DOC-" + UUID.randomUUID());

            List<Customer> page = inTransaction(() -> repository.findPage(0, 1000));

            assertTrue(page.indexOf(newer) < page.indexOf(older));
        }

        @Test
        @DisplayName("deve retornar lista vazia quando a página estiver fora do intervalo")
        void shouldReturnEmptyListWhenPageIsOutOfRange() {
            seed("DOC-" + UUID.randomUUID());

            List<Customer> page = inTransaction(() -> repository.findPage(1000, 10));

            assertTrue(page.isEmpty());
        }
    }

    private Customer seed(String document) {
        Customer customer = new Customer();
        customer.setFirstName("Maria");
        customer.setLastName("Silva");
        customer.setPhoneNumber("+5511999999999");
        customer.setDocument(document);
        customer.setDocumentType(DocumentType.CPF);
        return inTransaction(() -> repository.save(customer));
    }

    private <T> T inTransaction(Supplier<Uni<T>> action) {
        try {
            return VertxContextSupport.subscribeAndAwait(() -> Panache.withTransaction(action));
        } catch (Throwable exception) {
            throw new IllegalStateException("Falha ao preparar dados de teste", exception);
        }
    }
}
