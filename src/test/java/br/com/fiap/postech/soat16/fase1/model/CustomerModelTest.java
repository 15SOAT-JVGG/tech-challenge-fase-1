package br.com.fiap.postech.soat16.fase1.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.model.enums.DocumentType;

@DisplayName("Customer model — Unit Tests")
class CustomerModelTest {

    @Test
    @DisplayName("should allow setting basic fields")
    void shouldAllowSettingBasicFields() {
        Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@example.com");
        customer.setPhoneNumber("5511987654321");

        assertEquals("John", customer.getFirstName());
        assertEquals("Doe", customer.getLastName());
        assertEquals("john.doe@example.com", customer.getEmail());
        assertEquals("5511987654321", customer.getPhoneNumber());
    }

    @Test
    @DisplayName("equality is based on customerId")
    void equalityBasedOnId() {
        UUID id = UUID.randomUUID();
        Customer c1 = new Customer(id, "John", "Doe", "a@a.com", "111", "52998224725", DocumentType.CPF);
        Customer c2 = new Customer(id, "Jane", "Smith", "b@b.com", "222", "11222333000181", DocumentType.CNPJ);
        Customer c3 = new Customer(UUID.randomUUID(), "John", "Doe", "a@a.com", "111", "52998224725", DocumentType.CPF);

        assertEquals(c1, c2);
        assertNotEquals(c1, c3);
    }
}
