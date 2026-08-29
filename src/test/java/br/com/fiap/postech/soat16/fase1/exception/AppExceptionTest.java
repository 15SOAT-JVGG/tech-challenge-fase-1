package br.com.fiap.postech.soat16.fase1.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.EstimateNotFoundException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.WorkOrderNotFoundException;

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

    @Test
    @DisplayName("WorkOrderNotFoundException() carries a generic NOT_FOUND message")
    void workOrderNotFoundWithoutIdHasGenericMessage() {
        WorkOrderNotFoundException ex = new WorkOrderNotFoundException();

        assertEquals(ErrorType.NOT_FOUND, ex.getType());
        assertEquals("WORK_ORDER_NOT_FOUND", ex.getCode());
        assertEquals("Work order not found", ex.getMessage());
    }

    @Test
    @DisplayName("WorkOrderNotFoundException(UUID) includes the id in the message")
    void workOrderNotFoundWithIdIncludesItInMessage() {
        UUID id = UUID.randomUUID();
        WorkOrderNotFoundException ex = new WorkOrderNotFoundException(id);

        assertEquals(ErrorType.NOT_FOUND, ex.getType());
        assertTrue(ex.getMessage().contains(id.toString()));
    }

    @Test
    @DisplayName("EstimateNotFoundException() carries a generic NOT_FOUND message")
    void estimateNotFoundWithoutIdHasGenericMessage() {
        EstimateNotFoundException ex = new EstimateNotFoundException();

        assertEquals(ErrorType.NOT_FOUND, ex.getType());
        assertEquals("ESTIMATE_NOT_FOUND", ex.getCode());
        assertEquals("Estimate not found", ex.getMessage());
    }

    @Test
    @DisplayName("EstimateNotFoundException(UUID) includes the id in the message")
    void estimateNotFoundWithIdIncludesItInMessage() {
        UUID id = UUID.randomUUID();
        EstimateNotFoundException ex = new EstimateNotFoundException(id);

        assertEquals(ErrorType.NOT_FOUND, ex.getType());
        assertTrue(ex.getMessage().contains(id.toString()));
    }

    @Test
    @DisplayName("AppException(message, errorCode, errorType, cause) carries the cause, type and code")
    void constructorWithCauseCarriesAllFields() {
        RuntimeException cause = new RuntimeException("root cause");
        AppException ex = new AppException("failed", CustomerErrorCode.CUSTOMER_NOT_FOUND, ErrorType.NOT_FOUND, cause) {
        };

        assertEquals("failed", ex.getMessage());
        assertEquals(cause, ex.getCause());
        assertEquals(ErrorType.NOT_FOUND, ex.getType());
        assertEquals("CUSTOMER_NOT_FOUND", ex.getCode());
    }
}
