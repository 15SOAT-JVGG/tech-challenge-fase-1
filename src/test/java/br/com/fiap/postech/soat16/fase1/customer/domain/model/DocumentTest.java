package br.com.fiap.postech.soat16.fase1.customer.domain.model;

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

import br.com.fiap.postech.soat16.fase1.customer.domain.exception.InvalidDocumentException;
import br.com.fiap.postech.soat16.fase1.customer.domain.model.enums.DocumentType;

@DisplayName("Objeto de valor Document — Testes unitários")
class DocumentTest {

    @Nested
    @DisplayName("CPF")
    class Cpf {

        @Test
        @DisplayName("deve aceitar CPF válido e normalizar para somente dígitos")
        void shouldAcceptAndNormalizeValidCpf() {
            Document document = Document.of("529.982.247-25");

            assertEquals("52998224725", document.getValue());
            assertEquals(DocumentType.CPF, document.getType());
        }

        @Test
        @DisplayName("deve aceitar CPF válido sem máscara")
        void shouldAcceptValidCpfWithoutMask() {
            assertTrue(Document.isValid("52998224725"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"52998224724", "11111111111", "123", "00000000000"})
        @DisplayName("deve rejeitar CPF inválido, repetido ou com tamanho incorreto")
        void shouldRejectInvalidCpf(String invalid) {
            assertFalse(Document.isValid(invalid));
        }
    }

    @Nested
    @DisplayName("CNPJ")
    class Cnpj {

        @Test
        @DisplayName("deve aceitar CNPJ válido e normalizar para somente dígitos")
        void shouldAcceptAndNormalizeValidCnpj() {
            Document document = Document.of("11.222.333/0001-81");

            assertEquals("11222333000181", document.getValue());
            assertEquals(DocumentType.CNPJ, document.getType());
        }

        @ParameterizedTest
        @ValueSource(strings = {"11222333000182", "11111111111111", "1122233300018"})
        @DisplayName("deve rejeitar CNPJ inválido, repetido ou com tamanho incorreto")
        void shouldRejectInvalidCnpj(String invalid) {
            assertFalse(Document.isValid(invalid));
        }
    }

    @Nested
    @DisplayName("entrada inválida")
    class Invalid {

        @Test
        @DisplayName("deve lançar InvalidDocumentException para documento malformado")
        void shouldThrowForMalformedDocument() {
            assertThrows(InvalidDocumentException.class, () -> Document.of("abc"));
        }

        @Test
        @DisplayName("deve considerar nulo como inválido")
        void shouldRejectNull() {
            assertFalse(Document.isValid(null));
        }
    }

    @Nested
    @DisplayName("igualdade")
    class Equality {

        @Test
        @DisplayName("deve considerar iguais os documentos com o mesmo valor normalizado")
        void shouldCompareDocumentsByNormalizedValue() {
            Document first = Document.of("529.982.247-25");
            Document second = Document.of("52998224725");

            assertEquals(first, second);
            assertEquals(first.hashCode(), second.hashCode());
            assertEquals("52998224725", first.toString());
        }

        @Test
        @DisplayName("deve considerar diferentes os documentos com valores distintos")
        void shouldDifferentiateDistinctDocuments() {
            Document cpf = Document.of("52998224725");
            Document cnpj = Document.of("11222333000181");

            assertNotEquals(cpf, cnpj);
            assertNotEquals(null, cpf);
            assertNotEquals("52998224725", cpf);
        }
    }
}
