package br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.dto.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Filtro de veículos — testes unitários")
class VehicleFilterDtoTest {

    @Test
    @DisplayName("retorna nulo quando a placa não foi informada")
    void returnsNullWhenPlateIsMissing() throws ReflectiveOperationException {
        assertNull(dtoWithLicensePlate(null).getLicensePlate());
    }

    @Test
    @DisplayName("remove o hífen e converte para maiúsculas")
    void stripsHyphenAndUppercases() throws ReflectiveOperationException {
        assertEquals("ABC1234", dtoWithLicensePlate("abc-1234").getLicensePlate());
    }

    @Test
    @DisplayName("converte para maiúsculas quando não há hífen")
    void uppercasesWithoutHyphen() throws ReflectiveOperationException {
        assertEquals("ABC1234", dtoWithLicensePlate("abc1234").getLicensePlate());
    }

    @Test
    @DisplayName("mantém uma placa que já está normalizada")
    void keepsNormalizedPlate() throws ReflectiveOperationException {
        assertEquals("ABC1234", dtoWithLicensePlate("ABC1234").getLicensePlate());
    }

    @Test
    @DisplayName("remove todos os hífens")
    void stripsAllHyphens() throws ReflectiveOperationException {
        assertEquals(
                "ABC1234",
                dtoWithLicensePlate("A-B-C-1-2-3-4").getLicensePlate());
    }

    @SuppressWarnings("java:S3011")
    private VehicleFilterDto dtoWithLicensePlate(String value)
            throws ReflectiveOperationException {
        VehicleFilterDto dto = new VehicleFilterDto();
        Field field = VehicleFilterDto.class.getDeclaredField("licensePlate");
        field.setAccessible(true);
        field.set(dto, value);
        return dto;
    }
}
