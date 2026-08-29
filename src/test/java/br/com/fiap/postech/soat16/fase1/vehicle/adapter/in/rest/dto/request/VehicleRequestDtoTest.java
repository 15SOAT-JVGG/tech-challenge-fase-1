package br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.dto.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.enums.VehicleType;

@DisplayName("Dados de entrada do veículo — testes unitários")
class VehicleRequestDtoTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Nested
    @DisplayName("normalização da placa")
    class Normalization {

        @Test
        @DisplayName("remove o hífen e converte para maiúsculas")
        void stripsHyphenAndUppercases() {
            assertEquals("ABC1234", dto("abc-1234").licensePlate());
        }

        @Test
        @DisplayName("converte para maiúsculas quando não há hífen")
        void uppercasesWithoutHyphen() {
            assertEquals("ABC1234", dto("abc1234").licensePlate());
        }

        @Test
        @DisplayName("mantém uma placa que já está normalizada")
        void keepsNormalizedPlate() {
            assertEquals("ABC1234", dto("ABC1234").licensePlate());
        }

        @Test
        @DisplayName("remove todos os hífens")
        void stripsAllHyphens() {
            assertEquals("ABC1234", dto("A-B-C-1-2-3-4").licensePlate());
        }

        @Test
        @DisplayName("mantém nulo para que a validação obrigatória o rejeite")
        void keepsNullPlate() {
            assertNull(dto(null).licensePlate());
        }
    }

    @Nested
    @DisplayName("validação da placa")
    class LicensePlateValidation {

        @Test
        @DisplayName("aceita o formato brasileiro antigo")
        void acceptsOldBrazilianFormat() {
            assertFalse(hasLicensePlateViolation(dto("ABC1234")));
        }

        @Test
        @DisplayName("aceita o formato Mercosul")
        void acceptsMercosulFormat() {
            assertFalse(hasLicensePlateViolation(dto("ABC1D23")));
        }

        @Test
        @DisplayName("rejeita uma placa malformada")
        void rejectsMalformedPlate() {
            assertTrue(hasLicensePlateViolation(dto("AB1234")));
        }

        private boolean hasLicensePlateViolation(VehicleRequestDto dto) {
            return validator.validate(dto).stream()
                    .map(ConstraintViolation::getPropertyPath)
                    .map(Object::toString)
                    .anyMatch("licensePlate"::equals);
        }
    }

    private VehicleRequestDto dto(String licensePlate) {
        return new VehicleRequestDto(
                UUID.randomUUID(),
                licensePlate,
                "Fiat",
                "Uno",
                "Branco",
                2020,
                1_000L,
                VehicleType.CAR);
    }

}
