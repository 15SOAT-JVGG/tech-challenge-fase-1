package br.com.fiap.postech.soat16.fase1.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.dto.request.CustomerRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.CustomerResponseDto;
import br.com.fiap.postech.soat16.fase1.model.Customer;
import br.com.fiap.postech.soat16.fase1.model.Document;
import br.com.fiap.postech.soat16.fase1.model.enums.DocumentType;

@DisplayName("CustomerMapper — Unit Tests")
class CustomerMapperTest {

    private CustomerMapper mapper;

    private static final Document VALID_CPF = Document.of("529.982.247-25");

    @BeforeEach
    void setUp() {
        mapper = new CustomerMapper() {
        };
    }

    @Test
    @DisplayName("toResponse returns null when entity is null")
    void toResponseNullWhenEntityNull() {
        assertNull(mapper.toResponse(null));
    }

    @Test
    @DisplayName("toResponse maps all fields correctly")
    void toResponseMapsAllFields() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        Customer entity = new Customer();
        entity.setId(id);
        entity.setFirstName("John");
        entity.setLastName("Doe");
        entity.setEmail("john@example.com");
        entity.setPhoneNumber("5511987654321");
        entity.setDocument(VALID_CPF.getValue());
        entity.setDocumentType(DocumentType.CPF);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        CustomerResponseDto result = mapper.toResponse(entity);

        assertNotNull(result);
        assertEquals(id, result.customerId());
        assertEquals("John", result.firstName());
        assertEquals("Doe", result.lastName());
        assertEquals("john@example.com", result.email());
        assertEquals("5511987654321", result.phoneNumber());
        assertEquals(VALID_CPF.getValue(), result.document());
        assertEquals("CPF", result.documentType());
        assertEquals(now, result.createdAt());
    }

    @Test
    @DisplayName("toEntity returns null when request is null")
    void toEntityNullWhenRequestNull() {
        assertNull(mapper.toEntity(null, VALID_CPF));
    }

    @Test
    @DisplayName("toEntity maps create request correctly")
    void toEntityMapsCreateRequest() {
        CustomerRequestDto request = new CustomerRequestDto("John", "Doe", "john@example.com", "5511987654321", "529.982.247-25");

        Customer result = mapper.toEntity(request, VALID_CPF);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("john@example.com", result.getEmail());
        assertEquals("5511987654321", result.getPhoneNumber());
        assertEquals(VALID_CPF.getValue(), result.getDocument());
        assertEquals(DocumentType.CPF, result.getDocumentType());
    }

    @Test
    @DisplayName("updateEntity applies changes from update request")
    void updateEntityAppliesChanges() {
        Customer entity = new Customer();
        entity.setId(UUID.randomUUID());
        entity.setFirstName("John");
        entity.setLastName("Doe");
        entity.setEmail("john@example.com");
        entity.setPhoneNumber("5511987654321");
        entity.setDocument(VALID_CPF.getValue());
        entity.setDocumentType(DocumentType.CPF);

        CustomerRequestDto request = new CustomerRequestDto("Jane", "Smith", "jane@example.com", "5511111111111", "529.982.247-25");

        mapper.updateEntity(entity, request);

        assertEquals("Jane", entity.getFirstName());
        assertEquals("Smith", entity.getLastName());
        assertEquals("jane@example.com", entity.getEmail());
        assertEquals("5511111111111", entity.getPhoneNumber());
        assertEquals(VALID_CPF.getValue(), entity.getDocument());
    }
}
