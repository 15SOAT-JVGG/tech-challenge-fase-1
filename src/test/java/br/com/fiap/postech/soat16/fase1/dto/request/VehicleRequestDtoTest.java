package br.com.fiap.postech.soat16.fase1.dto.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import br.com.fiap.postech.soat16.fase1.model.VehicleType;

@DisplayName("VehicleRequestDto — Unit Tests")
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

    private VehicleRequestDto dto(String licensePlate) {
        return new VehicleRequestDto(UUID.randomUUID(), licensePlate, "Fiat", "Uno", "Branco",
            2020, 1000L, VehicleType.CAR);
    }

    private boolean hasLicensePlateViolation(VehicleRequestDto dto) {
        return validator.validate(dto).stream()
            .map(ConstraintViolation::getPropertyPath)
            .map(Object::toString)
            .anyMatch("licensePlate"::equals);
    }

    @Nested
    @DisplayName("normalization")
    class Normalization {

        @Test
        @DisplayName("should strip hyphen and uppercase the plate")
        void shouldNormalize() {
            assertEquals("ABC1234", dto("abc-1234").licensePlate());
        }
    }

    @Nested
    @DisplayName("licensePlate validation")
    class LicensePlateValidation {

        @Test
        @DisplayName("should accept old Brazilian format AAA0000")
        void shouldAcceptOldFormat() {
            assertFalse(hasLicensePlateViolation(dto("ABC1234")));
        }

        @Test
        @DisplayName("should accept Mercosul format AAA0A00")
        void shouldAcceptMercosul() {
            assertFalse(hasLicensePlateViolation(dto("ABC1D23")));
        }

        @Test
        @DisplayName("should reject a malformed plate")
        void shouldRejectMalformed() {
            assertTrue(hasLicensePlateViolation(dto("AB1234")));
        }
    }
}
