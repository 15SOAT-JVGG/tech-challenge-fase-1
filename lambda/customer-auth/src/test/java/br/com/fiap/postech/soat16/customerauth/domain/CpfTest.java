package br.com.fiap.postech.soat16.customerauth.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.fiap.postech.soat16.customerauth.domain.exception.InvalidCpfException;

@DisplayName("Cpf — Testes unitários")
class CpfTest {

    @Nested
    @DisplayName("CPF válido")
    class Valid {

        @ParameterizedTest
        @ValueSource(strings = {"529.982.247-25", "52998224725", " 529 982 247 25 "})
        @DisplayName("deve normalizar para os onze dígitos, com ou sem máscara")
        void shouldNormalizeToDigits(String raw) {
            assertEquals("52998224725", Cpf.of(raw).digits());
        }

        @Test
        @DisplayName("deve aceitar CPF cujo dígito verificador calculado é zero")
        void shouldAcceptCheckDigitZero() {
            assertEquals("12345678305", Cpf.of("123.456.783-05").digits());
        }

        @Test
        @DisplayName("deve comparar pelo valor normalizado")
        void shouldCompareByDigits() {
            assertEquals(Cpf.of("529.982.247-25"), Cpf.of("52998224725"));
            assertEquals(Cpf.of("529.982.247-25").hashCode(), Cpf.of("52998224725").hashCode());
            assertNotEquals(Cpf.of("52998224725"), Cpf.of("11144477735"));
        }

        @Test
        @DisplayName("deve mascarar o CPF ao virar texto, para não vazar em log")
        void shouldMaskWhenPrinted() {
            String printed = Cpf.of("52998224725").toString();

            assertEquals("***.982.***-25", printed);
            assertFalse(printed.contains("52998224725"));
        }
    }

    @Nested
    @DisplayName("CPF inválido")
    class Invalid {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
            "529.982.247-24",
            "52998224715",
            "5299822472",
            "529982247251",
            "111.111.111-11",
            "11.222.333/0001-81",
            "abc"
        })
        @DisplayName("deve recusar dígito verificador errado, tamanho errado, dígitos repetidos e CNPJ")
        void shouldRejectInvalidCpf(String raw) {
            assertThrows(InvalidCpfException.class, () -> Cpf.of(raw));
        }
    }
}
