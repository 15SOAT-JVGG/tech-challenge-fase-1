package br.com.fiap.postech.soat16.fase1.mapper;

import org.mapstruct.Mapper;

import br.com.fiap.postech.soat16.fase1.dto.request.WorkerRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkerLoginResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkerResponseDto;
import br.com.fiap.postech.soat16.fase1.model.Worker;

@Mapper(componentModel = "cdi")
public interface WorkerMapper {

    default WorkerResponseDto toResponse(Worker entity) {
        if (entity == null) {
            return null;
        }
        return new WorkerResponseDto(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }

    default WorkerLoginResponseDto toLoginResponse(Worker entity) {
        return new WorkerLoginResponseDto(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                true
        );
    }

    default Worker toEntity(WorkerRequestDto request, String passwordHash) {
        var entity = new Worker();
        entity.setProfile(request.profile());
        entity.setFirstName(request.firstName());
        entity.setLastName(request.lastName());
        entity.setEmail(request.email());
        entity.setPhoneNumber(request.phoneNumber());
        entity.setPasswordHash(passwordHash);
        entity.setActive(true);
        return entity;
    }

    default void updateEntity(Worker entity, WorkerRequestDto request) {
        entity.setProfile(request.profile());
        entity.setFirstName(request.firstName());
        entity.setLastName(request.lastName());
        entity.setEmail(request.email());
        entity.setPhoneNumber(request.phoneNumber());
    }
}
