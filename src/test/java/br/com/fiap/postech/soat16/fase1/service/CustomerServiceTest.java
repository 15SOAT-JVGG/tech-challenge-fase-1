package br.com.fiap.postech.soat16.fase1.service;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponse;
import br.com.fiap.postech.soat16.fase1.dto.request.CustomerCreateRequest;
import br.com.fiap.postech.soat16.fase1.dto.request.CustomerUpdateRequest;
import br.com.fiap.postech.soat16.fase1.dto.response.CustomerResponse;
import br.com.fiap.postech.soat16.fase1.exception.CustomerNotFoundException;
import br.com.fiap.postech.soat16.fase1.exception.DuplicateDocumentException;
import br.com.fiap.postech.soat16.fase1.exception.InvalidDocumentException;
import br.com.fiap.postech.soat16.fase1.mapper.CustomerMapper;
import br.com.fiap.postech.soat16.fase1.model.Customer;
import br.com.fiap.postech.soat16.fase1.model.Document;
import br.com.fiap.postech.soat16.fase1.model.DocumentType;
import br.com.fiap.postech.soat16.fase1.repository.CustomerRepository;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService — Unit Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepository repository;

    @Mock
    private CustomerMapper mapper;

    private CustomerService service;

    private static final UUID FIXED_UUID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    private Customer entity;
    private CustomerResponse response;

    @BeforeEach
    void setUp() {
        service = new CustomerService(repository, mapper);

        entity = new Customer();
        entity.setCustomerId(FIXED_UUID);
        entity.setFirstName("John");
        entity.setLastName("Doe");
        entity.setEmail("john.doe@example.com");
        entity.setPhoneNumber("5511987654321");
        entity.setDocument("52998224725");
        entity.setDocumentType(DocumentType.CPF);

        response = new CustomerResponse();
        response.setCustomerId(FIXED_UUID);
        response.setFirstName("John");
        response.setLastName("Doe");
        response.setEmail("john.doe@example.com");
        response.setPhoneNumber("5511987654321");
        response.setDocument("52998224725");
        response.setDocumentType("CPF");
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return paginated response")
        void shouldReturnPaginatedResponse() {
            when(repository.findPage(0, 10)).thenReturn(Uni.createFrom().item(List.of(entity)));
            when(repository.count()).thenReturn(Uni.createFrom().item(1L));
            when(mapper.toResponse(entity)).thenReturn(response);

            PageableResponse<CustomerResponse> result = service.findAll(null, 0, 10).await().indefinitely();

            assertNotNull(result);
            assertEquals(1, result.content().size());
            assertEquals(1L, result.pagination().totalElements());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return customer response when found")
        void shouldReturnCustomerWhenFound() {
            when(repository.findByCustomerId(FIXED_UUID)).thenReturn(Uni.createFrom().item(entity));
            when(mapper.toResponse(entity)).thenReturn(response);

            CustomerResponse result = service.findById(FIXED_UUID).await().indefinitely();

            assertNotNull(result);
            assertEquals(FIXED_UUID, result.getCustomerId());
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when not found")
        void shouldThrowNotFoundWhenMissing() {
            when(repository.findByCustomerId(FIXED_UUID)).thenReturn(Uni.createFrom().nullItem());

            assertThrows(CustomerNotFoundException.class,
                    () -> service.findById(FIXED_UUID).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update entity and return response")
        void shouldUpdateAndReturn() {
            CustomerUpdateRequest request = new CustomerUpdateRequest("Jane", "Doe", "jane.doe@example.com", "5511987654321");

            when(repository.findByCustomerId(FIXED_UUID)).thenReturn(Uni.createFrom().item(entity));
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));
            when(mapper.toResponse(entity)).thenReturn(response);

            CustomerResponse result = service.update(FIXED_UUID, request).await().indefinitely();

            assertNotNull(result);
            verify(mapper).updateEntity(entity, request);
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when entity is missing")
        void shouldThrowNotFoundWhenMissing() {
            CustomerUpdateRequest request = new CustomerUpdateRequest("Jane", "Doe", "jane.doe@example.com", "5511987654321");

            when(repository.findByCustomerId(FIXED_UUID)).thenReturn(Uni.createFrom().nullItem());

            assertThrows(CustomerNotFoundException.class,
                    () -> service.update(FIXED_UUID, request).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete customer when found")
        void shouldDeleteWhenFound() {
            when(repository.deleteByCustomerId(FIXED_UUID)).thenReturn(Uni.createFrom().item(1L));

            assertDoesNotThrow(() -> service.delete(FIXED_UUID).await().indefinitely());
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when no record deleted")
        void shouldThrowWhenNoRecordDeleted() {
            when(repository.deleteByCustomerId(FIXED_UUID)).thenReturn(Uni.createFrom().item(0L));

            assertThrows(CustomerNotFoundException.class,
                    () -> service.delete(FIXED_UUID).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("create — document validation")
    class CreateWithDocument {

        private static final String VALID_CPF_MASKED = "529.982.247-25";
        private static final String VALID_CPF_DIGITS = "52998224725";
        private static final String VALID_CNPJ_MASKED = "11.222.333/0001-81";
        private static final String VALID_CNPJ_DIGITS = "11222333000181";
        private static final String PHONE = "11999999999";

        @Test
        @DisplayName("create_withValidCpf_persists — valid CPF, no duplicates, completes without error")
        void create_withValidCpf_persists() {
            CustomerCreateRequest request = new CustomerCreateRequest(
                    "João", "Silva", "joao@example.com", PHONE, VALID_CPF_MASKED);

            Customer customerEntity = new Customer();
            customerEntity.setCustomerId(UUID.randomUUID());
            customerEntity.setFirstName("João");
            customerEntity.setDocument(VALID_CPF_DIGITS);
            customerEntity.setDocumentType(DocumentType.CPF);

            when(repository.existsByDocument(VALID_CPF_DIGITS)).thenReturn(Uni.createFrom().item(false));
            when(mapper.toEntity(eq(request), any(Document.class))).thenReturn(customerEntity);
            when(repository.persist(customerEntity)).thenReturn(Uni.createFrom().item(customerEntity));

            assertDoesNotThrow(() -> service.create(request).await().indefinitely());
            verify(repository).persist(customerEntity);
        }

        @Test
        @DisplayName("create_withValidCnpj_persists — valid CNPJ, no duplicates, completes without error")
        void create_withValidCnpj_persists() {
            CustomerCreateRequest request = new CustomerCreateRequest(
                    "Empresa", "LTDA", "empresa@example.com", PHONE, VALID_CNPJ_MASKED);

            Customer customerEntity = new Customer();
            customerEntity.setCustomerId(UUID.randomUUID());
            customerEntity.setFirstName("Empresa");
            customerEntity.setDocument(VALID_CNPJ_DIGITS);
            customerEntity.setDocumentType(DocumentType.CNPJ);

            when(repository.existsByDocument(VALID_CNPJ_DIGITS)).thenReturn(Uni.createFrom().item(false));
            when(mapper.toEntity(eq(request), any(Document.class))).thenReturn(customerEntity);
            when(repository.persist(customerEntity)).thenReturn(Uni.createFrom().item(customerEntity));

            assertDoesNotThrow(() -> service.create(request).await().indefinitely());
            verify(repository).persist(customerEntity);
        }

        @Test
        @DisplayName("create_withInvalidDocument_throwsInvalidDocumentException — throws before any repo call")
        void create_withInvalidDocument_throwsInvalidDocumentException() {
            CustomerCreateRequest request = new CustomerCreateRequest(
                    "João", "Silva", "joao@example.com", PHONE, "123");

            assertThrows(InvalidDocumentException.class,
                    () -> service.create(request).await().indefinitely());

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("create_withDuplicateDocument_throwsDuplicateDocumentException — existsByDocument returns true")
        void create_withDuplicateDocument_throwsDuplicateDocumentException() {
            CustomerCreateRequest request = new CustomerCreateRequest(
                    "João", "Silva", "joao@example.com", PHONE, VALID_CPF_MASKED);

            when(repository.existsByDocument(VALID_CPF_DIGITS)).thenReturn(Uni.createFrom().item(true));

            assertThrows(DuplicateDocumentException.class,
                    () -> service.create(request).await().indefinitely());

            verify(repository, never()).persist(any(Customer.class));
        }

    }

    @Nested
    @DisplayName("findByDocument — document lookup")
    class FindByDocumentTests {

        private static final String VALID_CPF_MASKED = "529.982.247-25";
        private static final String VALID_CPF_DIGITS = "52998224725";

        @Test
        @DisplayName("findByDocument_withValidDocument_returnsCustomer — returns response with normalized document")
        void findByDocument_withValidDocument_returnsCustomer() {
            Customer customerEntity = new Customer();
            customerEntity.setCustomerId(UUID.randomUUID());
            customerEntity.setFirstName("João");
            customerEntity.setDocument(VALID_CPF_DIGITS);
            customerEntity.setDocumentType(DocumentType.CPF);

            CustomerResponse expectedResponse = new CustomerResponse();
            expectedResponse.setCustomerId(customerEntity.getCustomerId());
            expectedResponse.setFirstName("João");
            expectedResponse.setDocument(VALID_CPF_DIGITS);
            expectedResponse.setDocumentType(DocumentType.CPF.name());

            when(repository.findByDocument(VALID_CPF_DIGITS)).thenReturn(Uni.createFrom().item(customerEntity));
            when(mapper.toResponse(customerEntity)).thenReturn(expectedResponse);

            CustomerResponse result = service.findByDocument(VALID_CPF_MASKED).await().indefinitely();

            assertNotNull(result);
            assertEquals(VALID_CPF_DIGITS, result.getDocument());
        }

        @Test
        @DisplayName("findByDocument_withInvalidDocument_throwsInvalidDocumentException — throws before any repo call")
        void findByDocument_withInvalidDocument_throwsInvalidDocumentException() {
            assertThrows(InvalidDocumentException.class,
                    () -> service.findByDocument("abc").await().indefinitely());

            verifyNoInteractions(repository);
        }
    }
}
