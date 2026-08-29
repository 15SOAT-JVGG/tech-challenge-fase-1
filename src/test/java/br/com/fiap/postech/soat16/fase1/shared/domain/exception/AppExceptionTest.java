package br.com.fiap.postech.soat16.fase1.shared.domain.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.customer.domain.exception.CustomerErrorCode;
import br.com.fiap.postech.soat16.fase1.customer.domain.exception.CustomerNotFoundException;
import br.com.fiap.postech.soat16.fase1.customer.domain.exception.DuplicateDocumentException;
import br.com.fiap.postech.soat16.fase1.customer.domain.exception.InvalidDocumentException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.EstimateNotFoundException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.WorkOrderNotFoundException;

@DisplayName("AppException hierarchy — Unit Tests")
class AppExceptionTest {

    @Test
    @DisplayName("CustomerNotFoundException carries its type and code")
    void customerNotFoundHasCorrectTypeAndCode() {
        CustomerNotFoundException exception = new CustomerNotFoundException();

        assertEquals(ErrorType.NOT_FOUND, exception.getType());
        assertEquals("CUSTOMER_NOT_FOUND", exception.getCode());
        assertEquals("Customer not found", exception.getMessage());
    }

    @Test
    @DisplayName("CustomerNotFoundException includes the document")
    void customerNotFoundWithDocumentIncludesItInMessage() {
        CustomerNotFoundException exception =
                new CustomerNotFoundException("52998224725");

        assertTrue(exception.getMessage().contains("52998224725"));
    }

    @Test
    @DisplayName("DuplicateDocumentException carries its type and code")
    void duplicateDocumentHasCorrectTypeAndCode() {
        DuplicateDocumentException exception = new DuplicateDocumentException();

        assertEquals(ErrorType.CONFLICT, exception.getType());
        assertEquals("DOCUMENT_ALREADY_EXISTS", exception.getCode());
    }

    @Test
    @DisplayName("InvalidDocumentException carries its type and code")
    void invalidDocumentHasCorrectTypeAndCode() {
        InvalidDocumentException exception = new InvalidDocumentException("123");

        assertEquals(ErrorType.VALIDATION, exception.getType());
        assertEquals("INVALID_DOCUMENT", exception.getCode());
        assertTrue(exception.getMessage().contains("123"));
    }

    @Test
    @DisplayName("CustomerErrorCode returns enum names")
    void customerErrorCodeReturnsName() {
        assertEquals(
                "CUSTOMER_NOT_FOUND",
                CustomerErrorCode.CUSTOMER_NOT_FOUND.getCode());
        assertEquals(
                "DOCUMENT_ALREADY_EXISTS",
                CustomerErrorCode.DOCUMENT_ALREADY_EXISTS.getCode());
        assertEquals(
                "INVALID_DOCUMENT",
                CustomerErrorCode.INVALID_DOCUMENT.getCode());
    }

    @Test
    @DisplayName("WorkOrderNotFoundException preserves type and code")
    void workOrderNotFoundWithoutIdHasGenericMessage() {
        WorkOrderNotFoundException exception = new WorkOrderNotFoundException();

        assertEquals(ErrorType.NOT_FOUND, exception.getType());
        assertEquals("WORK_ORDER_NOT_FOUND", exception.getCode());
        assertEquals("Work order not found", exception.getMessage());
    }

    @Test
    @DisplayName("WorkOrderNotFoundException includes the id")
    void workOrderNotFoundWithIdIncludesItInMessage() {
        UUID id = UUID.randomUUID();
        WorkOrderNotFoundException exception = new WorkOrderNotFoundException(id);

        assertTrue(exception.getMessage().contains(id.toString()));
    }

    @Test
    @DisplayName("EstimateNotFoundException preserves type and code")
    void estimateNotFoundWithoutIdHasGenericMessage() {
        EstimateNotFoundException exception = new EstimateNotFoundException();

        assertEquals(ErrorType.NOT_FOUND, exception.getType());
        assertEquals("ESTIMATE_NOT_FOUND", exception.getCode());
        assertEquals("Estimate not found", exception.getMessage());
    }

    @Test
    @DisplayName("EstimateNotFoundException includes the id")
    void estimateNotFoundWithIdIncludesItInMessage() {
        UUID id = UUID.randomUUID();
        EstimateNotFoundException exception = new EstimateNotFoundException(id);

        assertEquals(ErrorType.NOT_FOUND, exception.getType());
        assertTrue(exception.getMessage().contains(id.toString()));
    }

    @Test
    @DisplayName("constructor with cause preserves all fields")
    void constructorWithCauseCarriesAllFields() {
        RuntimeException cause = new RuntimeException("root cause");
        AppException exception = new AppException(
                "failed",
                CustomerErrorCode.CUSTOMER_NOT_FOUND,
                ErrorType.NOT_FOUND,
                cause) {
        };

        assertEquals("failed", exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(ErrorType.NOT_FOUND, exception.getType());
        assertEquals("CUSTOMER_NOT_FOUND", exception.getCode());
    }
}
