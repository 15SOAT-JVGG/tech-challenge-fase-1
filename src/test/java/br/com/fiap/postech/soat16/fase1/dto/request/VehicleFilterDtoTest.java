package br.com.fiap.postech.soat16.fase1.dto.request;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("VehicleFilterDto — Unit Tests")
class VehicleFilterDtoTest {

    private VehicleFilterDto dtoWithLicensePlate(String value) throws Exception {
        VehicleFilterDto dto = new VehicleFilterDto();
        Field field = VehicleFilterDto.class.getDeclaredField("licensePlate");
        field.setAccessible(true);
        field.set(dto, value);
        return dto;
    }

    @Nested
    @DisplayName("getLicensePlate")
    class GetLicensePlate {

        @Test
        @DisplayName("should return null when licensePlate is null")
        void shouldReturnNullWhenNull() throws Exception {
            VehicleFilterDto dto = dtoWithLicensePlate(null);
            assertNull(dto.getLicensePlate());
        }

        @Test
        @DisplayName("should remove dashes and uppercase")
        void shouldRemoveDashesAndUppercase() throws Exception {
            VehicleFilterDto dto = dtoWithLicensePlate("abc-1234");
            assertEquals("ABC1234", dto.getLicensePlate());
        }

        @Test
        @DisplayName("should uppercase without dashes")
        void shouldUppercaseWhenNoDashes() throws Exception {
            VehicleFilterDto dto = dtoWithLicensePlate("abc1234");
            assertEquals("ABC1234", dto.getLicensePlate());
        }

        @Test
        @DisplayName("should return unchanged when already normalized")
        void shouldReturnUnchangedWhenAlreadyNormalized() throws Exception {
            VehicleFilterDto dto = dtoWithLicensePlate("ABC1234");
            assertEquals("ABC1234", dto.getLicensePlate());
        }

        @Test
        @DisplayName("should handle multiple dashes")
        void shouldHandleMultipleDashes() throws Exception {
            VehicleFilterDto dto = dtoWithLicensePlate("A-B-C-1-2-3-4");
            assertEquals("ABC1234", dto.getLicensePlate());
        }
    }
}
