package br.com.fiap.postech.soat16.fase1.mapper;

import java.util.UUID;

import org.mapstruct.Mapper;

import br.com.fiap.postech.soat16.fase1.dto.request.AttendantCreateRequest;
import br.com.fiap.postech.soat16.fase1.dto.request.AttendantUpdateRequest;
import br.com.fiap.postech.soat16.fase1.dto.response.AttendantLoginResponse;
import br.com.fiap.postech.soat16.fase1.dto.response.AttendantResponse;
import br.com.fiap.postech.soat16.fase1.model.Attendant;

@Mapper(componentModel = "cdi")
public interface AttendantMapper {

    default AttendantResponse toResponse(Attendant entity) {
        if (entity == null) {
            return null;
        }
        return new AttendantResponse(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    default AttendantLoginResponse toLoginResponse(Attendant entity) {
        return new AttendantLoginResponse(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                true
        );
    }

    default Attendant toEntity(AttendantCreateRequest request, String passwordHash) {
        if (request == null) {
            return null;
        }
        var entity = new Attendant();
        entity.setId(UUID.randomUUID());
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setEmail(request.getEmail());
        entity.setPhoneNumber(request.getPhoneNumber());
        entity.setPasswordHash(passwordHash);
        entity.setActive(true);
        return entity;
    }

    default void updateEntity(Attendant entity, AttendantUpdateRequest request) {
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setEmail(request.getEmail());
        entity.setPhoneNumber(request.getPhoneNumber());
        entity.setActive(request.getActive());
    }
}
