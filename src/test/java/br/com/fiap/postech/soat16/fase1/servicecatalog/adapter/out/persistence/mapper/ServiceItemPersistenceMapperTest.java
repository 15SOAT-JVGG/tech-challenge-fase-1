package br.com.fiap.postech.soat16.fase1.servicecatalog.adapter.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.servicecatalog.adapter.out.persistence.entity.ServiceItemJpaEntity;
import br.com.fiap.postech.soat16.fase1.servicecatalog.domain.model.ServiceItem;

@DisplayName("ServiceItemPersistenceMapper — Unit Tests")
class ServiceItemPersistenceMapperTest {

    private static final UUID ID = UUID.randomUUID();
    private static final OffsetDateTime NOW = OffsetDateTime.now();

    private ServiceItem domain() {
        ServiceItem serviceItem = new ServiceItem();
        serviceItem.setId(ID);
        serviceItem.setName("Alinhamento");
        serviceItem.setDescription("Alinhamento e balanceamento");
        serviceItem.setBasePrice(new BigDecimal("120.00"));
        serviceItem.setEstimatedDurationMinutes(45);
        serviceItem.setActive(true);
        serviceItem.setCreatedAt(NOW);
        serviceItem.setUpdatedAt(NOW);
        serviceItem.setCreatedBy("system");
        serviceItem.setUpdatedBy("system");
        return serviceItem;
    }

    @Test
    @DisplayName("round trip preserves every field, including auditing")
    void roundTripPreservesFields() {
        ServiceItem result = ServiceItemPersistenceMapper.toDomain(
                ServiceItemPersistenceMapper.toJpaEntity(domain()));

        assertEquals(ID, result.getId());
        assertEquals("Alinhamento", result.getName());
        assertEquals("Alinhamento e balanceamento", result.getDescription());
        assertEquals(new BigDecimal("120.00"), result.getBasePrice());
        assertEquals(45, result.getEstimatedDurationMinutes());
        assertTrue(result.isActive());
        assertEquals(NOW, result.getCreatedAt());
        assertEquals("system", result.getCreatedBy());
    }

    @Test
    @DisplayName("copyState overwrites mutable fields but keeps the entity id")
    void copyStateKeepsEntityId() {
        ServiceItemJpaEntity entity = new ServiceItemJpaEntity();
        UUID persistedId = UUID.randomUUID();
        entity.setId(persistedId);

        ServiceItemPersistenceMapper.copyState(domain(), entity);

        assertEquals(persistedId, entity.getId());
        assertEquals("Alinhamento", entity.getName());
        assertEquals(45, entity.getEstimatedDurationMinutes());
    }

    @Test
    @DisplayName("toJpaReference carries only the identity")
    void jpaReferenceCarriesOnlyIdentity() {
        ServiceItemJpaEntity reference = ServiceItemPersistenceMapper.toJpaReference(ID);

        assertEquals(ID, reference.getId());
        assertNull(reference.getName());
    }

    @Test
    @DisplayName("null inputs map to null")
    void nullInputsMapToNull() {
        assertNull(ServiceItemPersistenceMapper.toDomain(null));
        assertNull(ServiceItemPersistenceMapper.toJpaEntity(null));
        assertNull(ServiceItemPersistenceMapper.toJpaReference(null));
    }
}
