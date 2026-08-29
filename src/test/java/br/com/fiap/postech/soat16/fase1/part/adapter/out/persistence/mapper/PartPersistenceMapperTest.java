package br.com.fiap.postech.soat16.fase1.part.adapter.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.part.adapter.out.persistence.entity.PartJpaEntity;
import br.com.fiap.postech.soat16.fase1.part.domain.model.Part;
import br.com.fiap.postech.soat16.fase1.part.domain.model.enums.PartType;

@DisplayName("PartPersistenceMapper — Unit Tests")
class PartPersistenceMapperTest {

    private static final UUID ID = UUID.randomUUID();
    private static final LocalDateTime NOW = LocalDateTime.now();

    private Part domain() {
        return Part.restore(
                ID,
                "Filtro de oleo",
                "Filtro compativel",
                new BigDecimal("59.90"),
                12,
                "UN",
                3,
                PartType.PART,
                NOW,
                NOW);
    }

    @Test
    @DisplayName("round trip preserves every field, including generated timestamps")
    void roundTripPreservesFields() {
        Part result = PartPersistenceMapper.toDomain(
                PartPersistenceMapper.toJpaEntity(domain()));

        assertEquals(ID, result.getId());
        assertEquals("Filtro de oleo", result.getName());
        assertEquals("Filtro compativel", result.getDescription());
        assertEquals(new BigDecimal("59.90"), result.getUnitPrice());
        assertEquals(12, result.getStockQuantity());
        assertEquals("UN", result.getUnit());
        assertEquals(3, result.getMinimumStock());
        assertEquals(PartType.PART, result.getPartType());
        assertEquals(NOW, result.getCreatedAt());
        assertEquals(NOW, result.getUpdatedAt());
    }

    @Test
    @DisplayName("copyState carries stock changes without touching id or version")
    void copyStateCarriesStockChanges() {
        PartJpaEntity entity = new PartJpaEntity();
        UUID persistedId = UUID.randomUUID();
        entity.setId(persistedId);
        entity.setVersion(7L);

        Part part = domain();
        part.decreaseStock(2);
        PartPersistenceMapper.copyState(part, entity);

        assertEquals(persistedId, entity.getId());
        assertEquals(7L, entity.getVersion());
        assertEquals(10, entity.getStockQuantity());
    }

    @Test
    @DisplayName("null inputs map to null")
    void nullInputsMapToNull() {
        assertNull(PartPersistenceMapper.toDomain(null));
        assertNull(PartPersistenceMapper.toJpaEntity(null));
    }
}
