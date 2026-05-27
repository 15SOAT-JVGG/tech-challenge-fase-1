package br.com.fiap.postech.soat16.fase1.mapper;

import br.com.fiap.postech.soat16.fase1.dto.request.CustomerCreateRequest;
import br.com.fiap.postech.soat16.fase1.dto.request.CustomerUpdateRequest;
import br.com.fiap.postech.soat16.fase1.dto.response.CustomerResponse;
import br.com.fiap.postech.soat16.fase1.model.Customer;
import org.mapstruct.Mapper;

import java.util.UUID;

@Mapper(componentModel = "cdi")
public interface CustomerMapper {

    default CustomerResponse toResponse(Customer entity) {
        if (entity == null) return null;
        return new CustomerResponse(
                entity.getCustomerId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    default Customer toEntity(CustomerCreateRequest request) {
        if (request == null) return null;
        var entity = new Customer();
        entity.setCustomerId(UUID.randomUUID());
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setEmail(request.getEmail());
        entity.setPhoneNumber(request.getPhoneNumber());
        return entity;
    }

    default void updateEntity(Customer entity, CustomerUpdateRequest request) {
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setEmail(request.getEmail());
        entity.setPhoneNumber(request.getPhoneNumber());
    }
}
