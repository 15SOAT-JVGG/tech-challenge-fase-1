package br.com.fiap.postech.soat16.fase1.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
        Customer c1 = new Customer(null, "John", "Doe", "a@a.com", "111");
        Customer c2 = new Customer(null, "Jane", "Smith", "b@b.com", "222");
        Customer c3 = new Customer(null, "John", "Doe", "a@a.com", "111");

        assertEquals(c1, c2);
        assertNotEquals(c1, c3);
    }
}
