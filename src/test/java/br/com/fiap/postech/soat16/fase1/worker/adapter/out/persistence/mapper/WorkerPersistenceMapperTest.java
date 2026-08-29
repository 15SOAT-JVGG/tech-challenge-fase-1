package br.com.fiap.postech.soat16.fase1.worker.adapter.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.worker.adapter.out.persistence.entity.WorkerJpaEntity;
import br.com.fiap.postech.soat16.fase1.worker.domain.model.Worker;
import br.com.fiap.postech.soat16.fase1.worker.domain.model.enums.WorkerProfile;

@DisplayName("WorkerPersistenceMapper — Unit Tests")
class WorkerPersistenceMapperTest {

    private static final UUID ID = UUID.randomUUID();
    private static final OffsetDateTime NOW = OffsetDateTime.now();

    private Worker domain() {
        Worker worker = Worker.create(
                WorkerProfile.MECHANIC, "Joao", "Souza", "joao@example.com", "11988887777", "hash");
        worker.setId(ID);
        worker.setCreatedAt(NOW);
        worker.setUpdatedAt(NOW);
        worker.setCreatedBy("system");
        worker.setUpdatedBy("system");
        return worker;
    }

    @Test
    @DisplayName("round trip preserves every field, including auditing")
    void roundTripPreservesFields() {
        Worker result = WorkerPersistenceMapper.toDomain(
                WorkerPersistenceMapper.toJpaEntity(domain()));

        assertEquals(ID, result.getId());
        assertEquals(WorkerProfile.MECHANIC, result.getProfile());
        assertEquals("Joao", result.getFirstName());
        assertEquals("Souza", result.getLastName());
        assertEquals("joao@example.com", result.getEmail());
        assertEquals("11988887777", result.getPhoneNumber());
        assertEquals("hash", result.getPasswordHash());
        assertTrue(result.isActive());
        assertEquals(NOW, result.getCreatedAt());
        assertEquals("system", result.getUpdatedBy());
    }

    @Test
    @DisplayName("copyState overwrites mutable fields but keeps the entity id")
    void copyStateKeepsEntityId() {
        WorkerJpaEntity entity = new WorkerJpaEntity();
        UUID persistedId = UUID.randomUUID();
        entity.setId(persistedId);

        WorkerPersistenceMapper.copyState(domain(), entity);

        assertEquals(persistedId, entity.getId());
        assertEquals("Joao", entity.getFirstName());
        assertTrue(entity.isActive());
    }

    @Test
    @DisplayName("toJpaReference carries only the identity")
    void jpaReferenceCarriesOnlyIdentity() {
        WorkerJpaEntity reference = WorkerPersistenceMapper.toJpaReference(ID);

        assertEquals(ID, reference.getId());
        assertNull(reference.getEmail());
    }

    @Test
    @DisplayName("null inputs map to null")
    void nullInputsMapToNull() {
        assertNull(WorkerPersistenceMapper.toDomain(null));
        assertNull(WorkerPersistenceMapper.toJpaEntity(null));
        assertNull(WorkerPersistenceMapper.toJpaReference(null));
    }
}
