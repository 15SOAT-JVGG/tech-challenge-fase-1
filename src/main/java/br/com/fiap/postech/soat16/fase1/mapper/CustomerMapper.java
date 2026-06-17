package br.com.fiap.postech.soat16.fase1.mapper;

import org.mapstruct.Mapper;

import br.com.fiap.postech.soat16.fase1.dto.request.CustomerRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.CustomerResponseDto;
import br.com.fiap.postech.soat16.fase1.model.Customer;
import br.com.fiap.postech.soat16.fase1.model.Document;

@Mapper(componentModel = "cdi")
public interface CustomerMapper {

    default CustomerResponseDto toResponse(Customer entity) {
        if (entity == null) {
            return null;
        }
        return new CustomerResponseDto(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getDocument(),
                entity.getDocumentType() != null ? entity.getDocumentType().name() : null,
                entity.getCreatedAt()
        );
    }

    default Customer toEntity(CustomerRequestDto request, Document document) {
        if (request == null) {
            return null;
        }
        var entity = new Customer();
        entity.setFirstName(request.firstName());
        entity.setLastName(request.lastName());
        entity.setEmail(request.email());
        entity.setPhoneNumber(request.phoneNumber());
        entity.setDocument(document.getValue());
        entity.setDocumentType(document.getType());
        return entity;
    }

    default void updateEntity(Customer entity, CustomerRequestDto request) {
        entity.setFirstName(request.firstName());
        entity.setLastName(request.lastName());
        entity.setEmail(request.email());
        entity.setPhoneNumber(request.phoneNumber());
    }
}
