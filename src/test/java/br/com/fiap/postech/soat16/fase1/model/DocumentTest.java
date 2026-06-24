package br.com.fiap.postech.soat16.fase1.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.fiap.postech.soat16.fase1.exception.InvalidDocumentException;
import br.com.fiap.postech.soat16.fase1.model.enums.DocumentType;

@DisplayName("Document value object — Unit Tests")
class DocumentTest {

    @Nested
    @DisplayName("CPF")
    class Cpf {

        @Test
        @DisplayName("accepts a valid CPF and normalizes to digits only")
        void acceptsValidCpf() {
            Document document = Document.of("529.982.247-25");

            assertEquals("52998224725", document.getValue());
            assertEquals(DocumentType.CPF, document.getType());
        }

        @Test
        @DisplayName("accepts a valid CPF without mask")
        void acceptsValidCpfWithoutMask() {
            assertTrue(Document.isValid("52998224725"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"52998224724", "11111111111", "123", "00000000000"})
        @DisplayName("rejects CPF with wrong check digits, repeated digits or wrong length")
        void rejectsInvalidCpf(String invalid) {
            assertFalse(Document.isValid(invalid));
        }
    }

    @Nested
    @DisplayName("CNPJ")
    class Cnpj {

        @Test
        @DisplayName("accepts a valid CNPJ and normalizes to digits only")
        void acceptsValidCnpj() {
            Document document = Document.of("11.222.333/0001-81");

            assertEquals("11222333000181", document.getValue());
            assertEquals(DocumentType.CNPJ, document.getType());
        }

        @ParameterizedTest
        @ValueSource(strings = {"11222333000182", "11111111111111", "1122233300018"})
        @DisplayName("rejects CNPJ with wrong check digits, repeated digits or wrong length")
        void rejectsInvalidCnpj(String invalid) {
            assertFalse(Document.isValid(invalid));
        }
    }

    @Nested
    @DisplayName("invalid input")
    class Invalid {

        @Test
        @DisplayName("of() throws InvalidDocumentException for a malformed document")
        void throwsForMalformedDocument() {
            assertThrows(InvalidDocumentException.class, () -> Document.of("abc"));
        }

        @Test
        @DisplayName("null is not valid")
        void nullIsNotValid() {
            assertFalse(Document.isValid(null));
        }
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        @DisplayName("documents are equal when normalized value matches")
        void equalByValue() {
            Document a = Document.of("529.982.247-25");
            Document b = Document.of("52998224725");

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
            assertEquals("52998224725", a.toString());
        }

        @Test
        @DisplayName("different documents are not equal")
        void notEqualWhenDifferent() {
            Document cpf = Document.of("52998224725");
            Document cnpj = Document.of("11222333000181");

            assertNotEquals(cpf, cnpj);
            assertNotEquals(null, cpf);
            assertNotEquals("52998224725", cpf);
        }
    }
}
