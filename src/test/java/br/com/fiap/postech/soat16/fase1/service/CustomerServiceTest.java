package br.com.fiap.postech.soat16.fase1.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.pagination.PaginationDto;
import br.com.fiap.postech.soat16.fase1.dto.request.CustomerCreateRequest;
import br.com.fiap.postech.soat16.fase1.dto.request.CustomerUpdateRequest;
import br.com.fiap.postech.soat16.fase1.dto.response.CustomerResponse;
import br.com.fiap.postech.soat16.fase1.exception.CustomerNotFoundException;
import br.com.fiap.postech.soat16.fase1.exception.DuplicatePhoneNumberException;
import br.com.fiap.postech.soat16.fase1.mapper.CustomerMapper;
import br.com.fiap.postech.soat16.fase1.model.Customer;
import br.com.fiap.postech.soat16.fase1.repository.CustomerRepository;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService — Unit Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepository repository;

    @Mock
    private CustomerMapper mapper;

    private CustomerService service;

    private Customer entity;
    private CustomerResponse response;

    @BeforeEach
    void setUp() {
        service = new CustomerService(repository, mapper);
        entity = new Customer(null, "John", "Doe", "john.doe@example.com", "5511987654321");
        response = new CustomerResponse(null, "John", "Doe", "john.doe@example.com", "5511987654321", null, null);
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return paginated response")
        void shouldReturnPaginatedResponse() {
            PaginationDto paginationDto = new PaginationDto(0, 10, 1L, 1, false, false);
            when(repository.findPage(0, 10)).thenReturn(Uni.createFrom().item(List.of(entity)));
            when(repository.count()).thenReturn(Uni.createFrom().item(1L));
            when(mapper.toResponse(entity)).thenReturn(response);

            PageableResponseDto<CustomerResponse> result = service.findAll(null, 0, 10).await().indefinitely();

            assertNotNull(result);
            assertEquals(1, result.content().size());
            assertEquals(1L, result.paginationDto().totalElements());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return customer response when found")
        void shouldReturnCustomerWhenFound() {
            when(repository.findByCustomerId(null)).thenReturn(Uni.createFrom().item(entity));
            when(mapper.toResponse(entity)).thenReturn(response);

            CustomerResponse result = service.findById(null).await().indefinitely();

            assertNotNull(result);
            assertEquals(null, result.getCustomerId());
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when not found")
        void shouldThrowNotFoundWhenMissing() {
            when(repository.findByCustomerId(null)).thenReturn(Uni.createFrom().nullItem());

            assertThrows(CustomerNotFoundException.class,
                    () -> service.findById(null).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should persist entity when phone is unique")
        void shouldPersistWhenPhoneIsUnique() {
            CustomerCreateRequest request = new CustomerCreateRequest("John", "Doe", "john.doe@example.com", "5511987654321");

            when(repository.existsByPhoneNumber("5511987654321")).thenReturn(Uni.createFrom().item(false));
            when(mapper.toEntity(request)).thenReturn(entity);
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));

            assertDoesNotThrow(() -> service.create(request).await().indefinitely());
            verify(repository).persist(entity);
        }

        @Test
        @DisplayName("should throw DuplicatePhoneNumberException when phone already exists")
        void shouldThrowDuplicateWhenPhoneExists() {
            CustomerCreateRequest request = new CustomerCreateRequest("John", "Doe", "john.doe@example.com", "5511987654321");

            when(repository.existsByPhoneNumber("5511987654321")).thenReturn(Uni.createFrom().item(true));

            assertThrows(DuplicatePhoneNumberException.class,
                    () -> service.create(request).await().indefinitely());

            verify(repository, never()).persist(any(Customer.class));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update entity and return response")
        void shouldUpdateAndReturn() {
            CustomerUpdateRequest request = new CustomerUpdateRequest("Jane", "Doe", "jane.doe@example.com", "5511987654321");

            when(repository.findByCustomerId(null)).thenReturn(Uni.createFrom().item(entity));
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));
            when(mapper.toResponse(entity)).thenReturn(response);

            CustomerResponse result = service.update(null, request).await().indefinitely();

            assertNotNull(result);
            verify(mapper).updateEntity(entity, request);
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when entity is missing")
        void shouldThrowNotFoundWhenMissing() {
            CustomerUpdateRequest request = new CustomerUpdateRequest("Jane", "Doe", "jane.doe@example.com", "5511987654321");

            when(repository.findByCustomerId(null)).thenReturn(Uni.createFrom().nullItem());

            assertThrows(CustomerNotFoundException.class,
                    () -> service.update(null, request).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete customer when found")
        void shouldDeleteWhenFound() {
            when(repository.deleteByCustomerId(null)).thenReturn(Uni.createFrom().item(1L));

            assertDoesNotThrow(() -> service.delete(null).await().indefinitely());
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when no record deleted")
        void shouldThrowWhenNoRecordDeleted() {
            when(repository.deleteByCustomerId(null)).thenReturn(Uni.createFrom().item(0L));

            assertThrows(CustomerNotFoundException.class,
                    () -> service.delete(null).await().indefinitely());
        }
    }
}
