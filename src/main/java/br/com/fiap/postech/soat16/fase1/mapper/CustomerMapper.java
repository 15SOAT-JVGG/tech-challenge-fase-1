package br.com.fiap.postech.soat16.fase1.mapper;

import br.com.fiap.postech.soat16.fase1.model.Document;
import org.mapstruct.Mapper;

import br.com.fiap.postech.soat16.fase1.dto.request.CustomerCreateRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.CustomerUpdateRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.CustomerResponseDto;
import br.com.fiap.postech.soat16.fase1.model.Customer;

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
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    default Customer toEntity(CustomerCreateRequestDto request, Document document) {
        if (request == null) {
            return null;
        }
        var entity = new Customer();
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setEmail(request.getEmail());
        entity.setPhoneNumber(request.getPhoneNumber());
        entity.setDocument(document.getValue());
        entity.setDocumentType(document.getType());
        return entity;
    }

    default void updateEntity(Customer entity, CustomerUpdateRequestDto request) {
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setEmail(request.getEmail());
        entity.setPhoneNumber(request.getPhoneNumber());
    }
}
