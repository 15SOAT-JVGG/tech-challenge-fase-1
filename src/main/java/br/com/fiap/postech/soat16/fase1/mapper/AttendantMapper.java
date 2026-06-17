package br.com.fiap.postech.soat16.fase1.mapper;

import org.mapstruct.Mapper;

import br.com.fiap.postech.soat16.fase1.dto.request.AttendantCreateRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.AttendantUpdateRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.AttendantLoginResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.AttendantResponseDto;
import br.com.fiap.postech.soat16.fase1.model.Attendant;

@Mapper(componentModel = "cdi")
public interface AttendantMapper {

    default AttendantResponseDto toResponse(Attendant entity) {
        if (entity == null) {
            return null;
        }
        return new AttendantResponseDto(
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

    default AttendantLoginResponseDto toLoginResponse(Attendant entity) {
        return new AttendantLoginResponseDto(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                true
        );
    }

    default Attendant toEntity(AttendantCreateRequestDto request, String passwordHash) {
        if (request == null) {
            return null;
        }
        var entity = new Attendant();
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setEmail(request.getEmail());
        entity.setPhoneNumber(request.getPhoneNumber());
        entity.setPasswordHash(passwordHash);
        entity.setActive(true);
        return entity;
    }

    default void updateEntity(Attendant entity, AttendantUpdateRequestDto request) {
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setEmail(request.getEmail());
        entity.setPhoneNumber(request.getPhoneNumber());
        entity.setActive(request.getActive());
    }
}
