package br.com.fiap.postech.soat16.fase1.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AppException hierarchy — Unit Tests")
class AppExceptionTest {

    @Test
    @DisplayName("CustomerNotFoundException carries NOT_FOUND type and CUSTOMER_NOT_FOUND code")
    void customerNotFoundHasCorrectTypeAndCode() {
        CustomerNotFoundException ex = new CustomerNotFoundException();

        assertEquals(ErrorType.NOT_FOUND, ex.getType());
        assertEquals("CUSTOMER_NOT_FOUND", ex.getCode());
        assertEquals("Customer not found", ex.getMessage());
    }

    @Test
    @DisplayName("CustomerNotFoundException(String) includes document in message")
    void customerNotFoundWithDocumentIncludesItInMessage() {
        CustomerNotFoundException ex = new CustomerNotFoundException("52998224725");
        assertTrue(ex.getMessage().contains("52998224725"));
    }

    @Test
    @DisplayName("DuplicateDocumentException carries CONFLICT type and DOCUMENT_ALREADY_EXISTS code")
    void duplicateDocumentHasCorrectTypeAndCode() {
        DuplicateDocumentException ex = new DuplicateDocumentException();

        assertEquals(ErrorType.CONFLICT, ex.getType());
        assertEquals("DOCUMENT_ALREADY_EXISTS", ex.getCode());
    }

    @Test
    @DisplayName("InvalidDocumentException carries VALIDATION type and INVALID_DOCUMENT code")
    void invalidDocumentHasCorrectTypeAndCode() {
        InvalidDocumentException ex = new InvalidDocumentException("123");

        assertEquals(ErrorType.VALIDATION, ex.getType());
        assertEquals("INVALID_DOCUMENT", ex.getCode());
        assertTrue(ex.getMessage().contains("123"));
    }

    @Test
    @DisplayName("CustomerErrorCode.getCode() returns enum name")
    void customerErrorCodeReturnsName() {
        assertEquals("CUSTOMER_NOT_FOUND", CustomerErrorCode.CUSTOMER_NOT_FOUND.getCode());
        assertEquals("DOCUMENT_ALREADY_EXISTS", CustomerErrorCode.DOCUMENT_ALREADY_EXISTS.getCode());
        assertEquals("INVALID_DOCUMENT", CustomerErrorCode.INVALID_DOCUMENT.getCode());
    }
}
