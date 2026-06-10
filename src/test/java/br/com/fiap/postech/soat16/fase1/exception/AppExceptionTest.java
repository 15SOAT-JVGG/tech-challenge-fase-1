package br.com.fiap.postech.soat16.fase1.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AppException hierarchy — Unit Tests")
class AppExceptionTest {

    @Test
    @DisplayName("CustomerNotFoundException carries NOT_FOUND type and CUSTOMER_NOT_FOUND code")
    void customerNotFoundHasCorrectTypeAndCode() {
        UUID id = UUID.randomUUID();
        CustomerNotFoundException ex = new CustomerNotFoundException(id);

        assertEquals(ErrorType.NOT_FOUND, ex.getType());
        assertEquals("CUSTOMER_NOT_FOUND", ex.getCode());
        assertTrue(ex.getMessage().contains(id.toString()));
    }

    @Test
    @DisplayName("CustomerNotFoundException(String) carries custom message")
    void customerNotFoundWithCustomMessage() {
        CustomerNotFoundException ex = new CustomerNotFoundException("custom message");
        assertEquals("custom message", ex.getMessage());
    }

    @Test
    @DisplayName("DuplicatePhoneNumberException carries CONFLICT type and PHONE_ALREADY_EXISTS code")
    void duplicatePhoneHasCorrectTypeAndCode() {
        DuplicatePhoneNumberException ex = new DuplicatePhoneNumberException();

        assertEquals(ErrorType.CONFLICT, ex.getType());
        assertEquals("PHONE_ALREADY_EXISTS", ex.getCode());
    }

    @Test
    @DisplayName("CustomerErrorCode.getCode() returns enum name")
    void customerErrorCodeReturnsName() {
        assertEquals("CUSTOMER_NOT_FOUND", CustomerErrorCode.CUSTOMER_NOT_FOUND.getCode());
        assertEquals("PHONE_ALREADY_EXISTS", CustomerErrorCode.PHONE_ALREADY_EXISTS.getCode());
        assertEquals("EMAIL_ALREADY_EXISTS", CustomerErrorCode.EMAIL_ALREADY_EXISTS.getCode());
        assertEquals("DUPLICATE_CUSTOMER", CustomerErrorCode.DUPLICATE_CUSTOMER.getCode());
    }
}
