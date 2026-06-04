package br.com.fiap.postech.soat16.fase1.dto.request;

import br.com.fiap.postech.soat16.fase1.model.VehicleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VehicleRequestDto — Unit Tests")
class VehicleRequestDtoTest {

    @Nested
    @DisplayName("licensePlate normalization (compact constructor)")
    class LicensePlateNormalization {

        @Test
        @DisplayName("should remove hyphen and convert to uppercase")
        void shouldRemoveHyphenAndUppercase() {
            var dto = new VehicleRequestDto(null, null, "abc-1234", "T", "M", "C", 2020, 0L, VehicleType.CARRO);
            assertEquals("ABC1234", dto.licensePlate());
        }

        @Test
        @DisplayName("should convert to uppercase when no hyphen present")
        void shouldUppercaseWithoutHyphen() {
            var dto = new VehicleRequestDto(null, null, "abc1234", "T", "M", "C", 2020, 0L, VehicleType.CARRO);
            assertEquals("ABC1234", dto.licensePlate());
        }

        @Test
        @DisplayName("should keep already normalized plate unchanged")
        void shouldKeepNormalizedPlateUnchanged() {
            var dto = new VehicleRequestDto(null, null, "ABC1234", "T", "M", "C", 2020, 0L, VehicleType.CARRO);
            assertEquals("ABC1234", dto.licensePlate());
        }

        @Test
        @DisplayName("should normalize Mercosul plate with hyphen")
        void shouldNormalizeMercosulPlateWithHyphen() {
            var dto = new VehicleRequestDto(null, null, "abc1d23", "T", "M", "C", 2020, 0L, VehicleType.CARRO);
            assertEquals("ABC1D23", dto.licensePlate());
        }

        @Test
        @DisplayName("should accept null licensePlate without throwing")
        void shouldAcceptNullWithoutThrowing() {
            var dto = new VehicleRequestDto(null, null, null, "T", "M", "C", 2020, 0L, VehicleType.CARRO);
            assertNull(dto.licensePlate());
        }
    }
}