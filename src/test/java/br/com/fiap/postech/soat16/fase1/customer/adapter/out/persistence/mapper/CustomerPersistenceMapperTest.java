package br.com.fiap.postech.soat16.fase1.customer.adapter.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.customer.adapter.out.persistence.entity.CustomerJpaEntity;
import br.com.fiap.postech.soat16.fase1.customer.domain.model.Customer;
import br.com.fiap.postech.soat16.fase1.customer.domain.model.enums.DocumentType;

@DisplayName("CustomerPersistenceMapper — Unit Tests")
class CustomerPersistenceMapperTest {

    private static final UUID ID = UUID.randomUUID();
    private static final OffsetDateTime NOW = OffsetDateTime.now();

    private Customer domain() {
        Customer customer = new Customer();
        customer.setId(ID);
        customer.setFirstName("Maria");
        customer.setLastName("Silva");
        customer.setEmail("maria@example.com");
        customer.setPhoneNumber("+5511999999999");
        customer.setDocument("12345678901");
        customer.setDocumentType(DocumentType.CPF);
        customer.setCreatedAt(NOW);
        customer.setUpdatedAt(NOW);
        customer.setCreatedBy("system");
        customer.setUpdatedBy("system");
        return customer;
    }

    @Test
    @DisplayName("round trip preserves every field, including auditing")
    void roundTripPreservesFields() {
        Customer original = domain();

        Customer result = CustomerPersistenceMapper.toDomain(
                CustomerPersistenceMapper.toJpaEntity(original));

        assertEquals(ID, result.getId());
        assertEquals("Maria", result.getFirstName());
        assertEquals("Silva", result.getLastName());
        assertEquals("maria@example.com", result.getEmail());
        assertEquals("+5511999999999", result.getPhoneNumber());
        assertEquals("12345678901", result.getDocument());
        assertEquals(DocumentType.CPF, result.getDocumentType());
        assertEquals(NOW, result.getCreatedAt());
        assertEquals(NOW, result.getUpdatedAt());
        assertEquals("system", result.getCreatedBy());
        assertEquals("system", result.getUpdatedBy());
    }

    @Test
    @DisplayName("copyState overwrites mutable fields but keeps the entity id")
    void copyStateKeepsEntityId() {
        CustomerJpaEntity entity = new CustomerJpaEntity();
        UUID persistedId = UUID.randomUUID();
        entity.setId(persistedId);

        CustomerPersistenceMapper.copyState(domain(), entity);

        assertEquals(persistedId, entity.getId());
        assertEquals("Maria", entity.getFirstName());
        assertEquals(DocumentType.CPF, entity.getDocumentType());
    }

    @Test
    @DisplayName("toJpaReference carries only the identity")
    void jpaReferenceCarriesOnlyIdentity() {
        CustomerJpaEntity reference = CustomerPersistenceMapper.toJpaReference(ID);

        assertEquals(ID, reference.getId());
        assertNull(reference.getFirstName());
    }

    @Test
    @DisplayName("null inputs map to null")
    void nullInputsMapToNull() {
        assertNull(CustomerPersistenceMapper.toDomain(null));
        assertNull(CustomerPersistenceMapper.toJpaEntity(null));
        assertNull(CustomerPersistenceMapper.toJpaReference(null));
    }
}
