package br.com.fiap.postech.soat16.fase1.model.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.model.Customer;

@DisplayName("AuditEntityListener — Unit Tests")
class AuditEntityListenerTest {

    private final AuditEntityListener listener = new AuditEntityListener();

    @Test
    @DisplayName("prePersist stamps createdAt, updatedAt and the system author")
    void prePersistStampsCreationFields() {
        Customer customer = new Customer();

        listener.prePersist(customer);

        assertNotNull(customer.getCreatedAt());
        assertNotNull(customer.getUpdatedAt());
        assertEquals("system", customer.getCreatedBy());
        assertEquals("system", customer.getUpdatedBy());
    }

    @Test
    @DisplayName("preUpdate refreshes updatedAt and updatedBy without touching createdAt/createdBy")
    void preUpdateRefreshesUpdateFieldsOnly() {
        Customer customer = new Customer();
        OffsetDateTime originalCreatedAt = OffsetDateTime.now().minusDays(1);
        customer.setCreatedAt(originalCreatedAt);
        customer.setCreatedBy("original-author");

        listener.preUpdate(customer);

        assertEquals(originalCreatedAt, customer.getCreatedAt());
        assertEquals("original-author", customer.getCreatedBy());
        assertEquals("system", customer.getUpdatedBy());
        assertTrue(customer.getUpdatedAt().isAfter(originalCreatedAt));
    }
}
