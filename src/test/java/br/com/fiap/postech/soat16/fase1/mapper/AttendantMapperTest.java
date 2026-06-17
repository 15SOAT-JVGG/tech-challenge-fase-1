package br.com.fiap.postech.soat16.fase1.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.dto.request.AttendantCreateRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.AttendantUpdateRequestDto;
import br.com.fiap.postech.soat16.fase1.model.Attendant;

@DisplayName("AttendantMapper - Unit Tests")
class AttendantMapperTest {

    private final AttendantMapper mapper = new AttendantMapper() {
    };

    @Test
    @DisplayName("should map entity to response")
    void shouldMapEntityToResponse() {
        UUID id = UUID.randomUUID();
        Attendant entity = new Attendant(id, "Ana", "Silva", "ana@example.com", "5511999999999", "hash", true);

        var response = mapper.toResponse(entity);

        assertEquals(id, response.getAttendantId());
        assertEquals("Ana", response.getFirstName());
        assertTrue(response.getActive());
    }

    @Test
    @DisplayName("should return null response when entity is null")
    void shouldReturnNullResponseWhenEntityIsNull() {
        assertNull(mapper.toResponse(null));
    }

    @Test
    @DisplayName("should map create request to entity")
    void shouldMapCreateRequestToEntity() {
        AttendantCreateRequestDto request = new AttendantCreateRequestDto(
                "Ana", "Silva", "ana@example.com", "5511999999999", "password123");

        Attendant entity = mapper.toEntity(request, "hash");

        assertEquals("Ana", entity.getFirstName());
        assertEquals("ana@example.com", entity.getEmail());
        assertEquals("hash", entity.getPasswordHash());
        assertTrue(entity.isActive());
    }

    @Test
    @DisplayName("should return null entity when request is null")
    void shouldReturnNullEntityWhenRequestIsNull() {
        assertNull(mapper.toEntity(null, "hash"));
    }

    @Test
    @DisplayName("should update entity")
    void shouldUpdateEntity() {
        Attendant entity = new Attendant(UUID.randomUUID(), "Ana", "Silva", "ana@example.com", null, "hash", true);
        AttendantUpdateRequestDto request = new AttendantUpdateRequestDto(
                "Maria", "Souza", "maria@example.com", "5511888888888", false);

        mapper.updateEntity(entity, request);

        assertEquals("Maria", entity.getFirstName());
        assertEquals("maria@example.com", entity.getEmail());
        assertFalse(entity.isActive());
    }
}
