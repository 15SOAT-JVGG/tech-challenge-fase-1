package br.com.fiap.postech.soat16.fase1.mapper;

import org.mapstruct.Mapper;

import br.com.fiap.postech.soat16.fase1.dto.request.AttendantRequestDto;
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
                entity.getCreatedAt()
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

    default Attendant toEntity(AttendantRequestDto request, String passwordHash) {
        var entity = new Attendant();
        entity.setFirstName(request.firstName());
        entity.setLastName(request.lastName());
        entity.setEmail(request.email());
        entity.setPhoneNumber(request.phoneNumber());
        entity.setPasswordHash(passwordHash);
        entity.setActive(true);
        return entity;
    }

    default void updateEntity(Attendant entity, AttendantRequestDto request) {
        entity.setFirstName(request.firstName());
        entity.setLastName(request.lastName());
        entity.setEmail(request.email());
        entity.setPhoneNumber(request.phoneNumber());
    }
}
