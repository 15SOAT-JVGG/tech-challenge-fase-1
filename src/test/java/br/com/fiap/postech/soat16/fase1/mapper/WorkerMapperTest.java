package br.com.fiap.postech.soat16.fase1.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.dto.request.WorkerRequestDto;
import br.com.fiap.postech.soat16.fase1.model.Worker;
import br.com.fiap.postech.soat16.fase1.model.WorkerProfile;

@DisplayName("WorkerMapper - Unit Tests")
class WorkerMapperTest {

    private final WorkerMapper mapper = new WorkerMapper() {
    };

    @Test
    @DisplayName("should map entity to response")
    void shouldMapEntityToResponse() {
        UUID id = UUID.randomUUID();
        Worker entity = new Worker(id, WorkerProfile.MECHANIC, "Ana", "Silva", "ana@example.com", "5511999999999", "hash", true);

        var response = mapper.toResponse(entity);

        assertEquals(id, response.workerId());
        assertEquals("Ana", response.firstName());
        assertTrue(response.active());
    }

    @Test
    @DisplayName("should return null response when entity is null")
    void shouldReturnNullResponseWhenEntityIsNull() {
        assertNull(mapper.toResponse(null));
    }

    @Test
    @DisplayName("should map entity to login response")
    void shouldMapEntityToLoginResponse() {
        UUID id = UUID.randomUUID();
        Worker entity = new Worker(id, WorkerProfile.MECHANIC, "Ana", "Silva", "ana@example.com", "5511999999999", "hash", true);

        var response = mapper.toLoginResponse(entity);

        assertEquals(id, response.workerId());
        assertEquals("Ana", response.firstName());
        assertEquals("Silva", response.lastName());
        assertEquals("ana@example.com", response.email());
        assertTrue(response.authenticated());
    }

    @Test
    @DisplayName("should map create request to entity")
    void shouldMapCreateRequestToEntity() {
        WorkerRequestDto request = new WorkerRequestDto(
                "Ana", "Silva", "ana@example.com", "5511999999999", "password123", WorkerProfile.MECHANIC);

        Worker entity = mapper.toEntity(request, "hash");

        assertEquals("Ana", entity.getFirstName());
        assertEquals("ana@example.com", entity.getEmail());
        assertEquals("hash", entity.getPasswordHash());
        assertTrue(entity.isActive());
    }

    @Test
    @DisplayName("should update entity")
    void shouldUpdateEntity() {
        Worker entity = new Worker(UUID.randomUUID(), WorkerProfile.MECHANIC, "Ana", "Silva", "ana@example.com", null, "hash", true);
        WorkerRequestDto request = new WorkerRequestDto(
                "Maria", "Souza", "maria@example.com", "5511888888888", "1234", WorkerProfile.MECHANIC);

        mapper.updateEntity(entity, request);

        assertEquals("Maria", entity.getFirstName());
        assertEquals("maria@example.com", entity.getEmail());
    }
}
