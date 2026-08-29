package br.com.fiap.postech.soat16.fase1.auth.adapter.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.auth.adapter.out.persistence.entity.AppUserJpaEntity;
import br.com.fiap.postech.soat16.fase1.auth.domain.model.AppUser;

@DisplayName("AppUserPersistenceMapper — Unit Tests")
class AppUserPersistenceMapperTest {

    private static final LocalDateTime NOW = LocalDateTime.now();

    @Test
    @DisplayName("round trip preserves every field, including the creation instant")
    void roundTripPreservesFields() {
        AppUser original = AppUser.restore(42L, "admin", "hash", "ADMIN", true, NOW);

        AppUser result = AppUserPersistenceMapper.toDomain(
                AppUserPersistenceMapper.toJpaEntity(original));

        assertEquals(42L, result.getId());
        assertEquals("admin", result.getUsername());
        assertEquals("hash", result.getPassword());
        assertEquals("ADMIN", result.getRole());
        assertTrue(result.isActive());
        assertEquals(NOW, result.getCreatedAt());
    }

    @Test
    @DisplayName("copyState carries deactivation but keeps the entity id")
    void copyStateCarriesDeactivation() {
        AppUserJpaEntity entity = new AppUserJpaEntity();
        entity.setId(9L);

        AppUser user = AppUser.restore(42L, "admin", "hash", "ADMIN", true, NOW);
        user.deactivate();
        AppUserPersistenceMapper.copyState(user, entity);

        assertEquals(9L, entity.getId());
        assertFalse(entity.getActive());
    }

    @Test
    @DisplayName("null inputs map to null")
    void nullInputsMapToNull() {
        assertNull(AppUserPersistenceMapper.toDomain(null));
        assertNull(AppUserPersistenceMapper.toJpaEntity(null));
    }
}
