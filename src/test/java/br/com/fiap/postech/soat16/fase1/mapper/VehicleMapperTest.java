package br.com.fiap.postech.soat16.fase1.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.dto.request.VehicleRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.VehicleResponseDto;
import br.com.fiap.postech.soat16.fase1.model.Customer;
import br.com.fiap.postech.soat16.fase1.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.model.VehicleType;

@DisplayName("VehicleMapper — Unit Tests")
class VehicleMapperTest {

    private final VehicleMapper mapper = new VehicleMapper() {};

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("should map all fields from entity to response")
        void shouldMapEntityToResponse() {
            UUID id = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();
            Customer customer = new Customer();
            customer.setId(customerId);
            Vehicle entity = new Vehicle(id, customer, "ABC1234", "Toyota", "Corolla", "Prata", 2020, 50000L, VehicleType.CAR);

            VehicleResponseDto result = mapper.toResponse(entity);

            assertNotNull(result);
            assertEquals(id, result.id());
            assertEquals("ABC1234", result.licensePlate());
            assertEquals("Toyota", result.manufacturer());
            assertEquals("Corolla", result.model());
            assertEquals("Prata", result.color());
            assertEquals(2020, result.year());
            assertEquals(50000L, result.kmDriven());
            assertEquals(VehicleType.CAR, result.type());
            assertEquals(customerId, result.customerId());
        }
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("should map all fields from dto to entity")
        void shouldMapDtoToEntity() {
            VehicleRequestDto dto = new VehicleRequestDto(null, "ABC1234", "Toyota", "Corolla", "Prata", 2020, 50000L, VehicleType.CAR);

            Vehicle result = mapper.toEntity(dto, null);

            assertNotNull(result);
            assertEquals("ABC1234", result.getLicensePlate());
            assertEquals("Toyota", result.getManufacturer());
            assertEquals("Corolla", result.getModel());
            assertEquals("Prata", result.getColor());
            assertEquals(2020, result.getYear());
            assertEquals(50000L, result.getKmDriven());
            assertEquals(VehicleType.CAR, result.getType());
        }
    }

    @Nested
    @DisplayName("updateEntity")
    class UpdateEntity {

        @Test
        @DisplayName("should overwrite all fields on existing entity")
        void shouldUpdateAllFields() {
            Vehicle entity = new Vehicle(UUID.randomUUID(), null, "ABC1234", "Toyota", "Corolla", "Prata", 2020, 50000L, VehicleType.CAR);
            VehicleRequestDto dto = new VehicleRequestDto(null, "XYZ9876", "Honda", "Civic", "Preto", 2022, 10000L, VehicleType.MOTOCYCLE);

            mapper.updateEntity(entity, dto);

            assertEquals("XYZ9876", entity.getLicensePlate());
            assertEquals("Honda", entity.getManufacturer());
            assertEquals("Civic", entity.getModel());
            assertEquals("Preto", entity.getColor());
            assertEquals(2022, entity.getYear());
            assertEquals(10000L, entity.getKmDriven());
            assertEquals(VehicleType.MOTOCYCLE, entity.getType());
        }
    }
}
