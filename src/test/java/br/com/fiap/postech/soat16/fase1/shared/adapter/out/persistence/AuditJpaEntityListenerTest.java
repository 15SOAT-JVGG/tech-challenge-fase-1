package br.com.fiap.postech.soat16.fase1.shared.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.customer.adapter.out.persistence.entity.CustomerJpaEntity;

@DisplayName("AuditJpaEntityListener — Unit Tests")
class AuditJpaEntityListenerTest {

    private final AuditJpaEntityListener listener = new AuditJpaEntityListener();

    @Test
    @DisplayName("prePersist stamps creation fields")
    void prePersistStampsCreationFields() {
        CustomerJpaEntity customer = new CustomerJpaEntity();

        listener.prePersist(customer);

        assertNotNull(customer.getCreatedAt());
        assertNotNull(customer.getUpdatedAt());
        assertEquals("system", customer.getCreatedBy());
        assertEquals("system", customer.getUpdatedBy());
    }

    @Test
    @DisplayName("preUpdate refreshes only update fields")
    void preUpdateRefreshesUpdateFieldsOnly() {
        CustomerJpaEntity customer = new CustomerJpaEntity();
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
