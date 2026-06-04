package br.com.fiap.postech.soat16.fase1.mapper;

import br.com.fiap.postech.soat16.fase1.dto.request.CustomerCreateRequest;
import br.com.fiap.postech.soat16.fase1.dto.request.CustomerUpdateRequest;
import br.com.fiap.postech.soat16.fase1.dto.response.CustomerResponse;
import br.com.fiap.postech.soat16.fase1.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CustomerMapper — Unit Tests")
class CustomerMapperTest {

    private CustomerMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CustomerMapper() {};
    }

    @Test
    @DisplayName("toResponse returns null when entity is null")
    void toResponseNullWhenEntityNull() {
        assertNull(mapper.toResponse(null));
    }

    @Test
    @DisplayName("toResponse maps all fields correctly")
    void toResponseMapsAllFields() {
        OffsetDateTime now = OffsetDateTime.now();
        Customer entity = new Customer(null, "John", "Doe", "john@example.com", "5511987654321");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        CustomerResponse result = mapper.toResponse(entity);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("john@example.com", result.getEmail());
        assertEquals("5511987654321", result.getPhoneNumber());
        assertEquals(now, result.getCreatedAt());
    }

    @Test
    @DisplayName("toEntity returns null when request is null")
    void toEntityNullWhenRequestNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    @DisplayName("toEntity maps create request correctly and generates UUID")
    void toEntityMapsCreateRequest() {
        CustomerCreateRequest request = new CustomerCreateRequest("John", "Doe", "john@example.com", "5511987654321");

        Customer result = mapper.toEntity(request);

        assertNotNull(result);
        assertNotNull(result.getCustomerId());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("john@example.com", result.getEmail());
        assertEquals("5511987654321", result.getPhoneNumber());
    }

    @Test
    @DisplayName("updateEntity applies changes from update request")
    void updateEntityAppliesChanges() {
        Customer entity = new Customer(null, "John", "Doe", "john@example.com", "5511987654321");
        CustomerUpdateRequest request = new CustomerUpdateRequest("Jane", "Smith", "jane@example.com", "5511111111111");

        mapper.updateEntity(entity, request);

        assertEquals("Jane", entity.getFirstName());
        assertEquals("Smith", entity.getLastName());
        assertEquals("jane@example.com", entity.getEmail());
        assertEquals("5511111111111", entity.getPhoneNumber());
    }
}
