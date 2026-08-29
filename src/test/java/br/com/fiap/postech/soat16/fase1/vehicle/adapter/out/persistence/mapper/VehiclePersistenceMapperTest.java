package br.com.fiap.postech.soat16.fase1.vehicle.adapter.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.customer.domain.model.Customer;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.out.persistence.entity.VehicleJpaEntity;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.enums.VehicleType;

@DisplayName("VehiclePersistenceMapper — Unit Tests")
class VehiclePersistenceMapperTest {

    private static final UUID VEHICLE_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final OffsetDateTime NOW = OffsetDateTime.now();

    private Vehicle domain() {
        Customer owner = new Customer();
        owner.setId(CUSTOMER_ID);

        Vehicle vehicle = Vehicle.create(
                owner, "ABC1D23", "Fiat", "Uno", "Branco", 2020, 10_000L, VehicleType.CAR);
        vehicle.setId(VEHICLE_ID);
        vehicle.setCreatedAt(NOW);
        vehicle.setUpdatedAt(NOW);
        vehicle.setCreatedBy("system");
        vehicle.setUpdatedBy("system");
        return vehicle;
    }

    @Test
    @DisplayName("round trip preserves fields and the owning customer id")
    void roundTripPreservesFields() {
        Vehicle result = VehiclePersistenceMapper.toDomain(
                VehiclePersistenceMapper.toJpaEntity(domain()));

        assertEquals(VEHICLE_ID, result.getId());
        assertEquals(CUSTOMER_ID, result.getCustomer().getId());
        assertEquals("ABC1D23", result.getLicensePlate());
        assertEquals("Fiat", result.getManufacturer());
        assertEquals("Uno", result.getModel());
        assertEquals("Branco", result.getColor());
        assertEquals(2020, result.getYear());
        assertEquals(10_000L, result.getKmDriven());
        assertEquals(VehicleType.CAR, result.getType());
        assertEquals(NOW, result.getCreatedAt());
    }

    @Test
    @DisplayName("a vehicle without an owner maps to a null customer")
    void vehicleWithoutOwnerMapsToNullCustomer() {
        Vehicle vehicle = domain();
        vehicle.setCustomer(null);

        VehicleJpaEntity entity = VehiclePersistenceMapper.toJpaEntity(vehicle);

        assertNull(entity.getCustomer());
        assertNull(VehiclePersistenceMapper.toDomain(entity).getCustomer());
    }

    @Test
    @DisplayName("copyState overwrites mutable fields but keeps id and owner")
    void copyStateKeepsIdentityAndOwner() {
        VehicleJpaEntity entity = VehiclePersistenceMapper.toJpaEntity(domain());
        Vehicle updated = domain();
        updated.update("XYZ9K88", "VW", "Gol", "Preto", 2022, 20_000L, VehicleType.CAR);

        VehiclePersistenceMapper.copyState(updated, entity);

        assertEquals(VEHICLE_ID, entity.getId());
        assertEquals(CUSTOMER_ID, entity.getCustomer().getId());
        assertEquals("XYZ9K88", entity.getLicensePlate());
        assertEquals("Gol", entity.getModel());
    }

    @Test
    @DisplayName("copyGeneratedState returns identity and auditing to the aggregate")
    void copyGeneratedStateFeedsBackIdentity() {
        VehicleJpaEntity entity = new VehicleJpaEntity();
        UUID generatedId = UUID.randomUUID();
        entity.setId(generatedId);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        entity.setCreatedBy("system");
        entity.setUpdatedBy("system");

        Vehicle vehicle = new Vehicle();
        VehiclePersistenceMapper.copyGeneratedState(entity, vehicle);

        assertEquals(generatedId, vehicle.getId());
        assertEquals(NOW, vehicle.getCreatedAt());
        assertEquals("system", vehicle.getCreatedBy());
    }

    @Test
    @DisplayName("null inputs map to null")
    void nullInputsMapToNull() {
        assertNull(VehiclePersistenceMapper.toDomain(null));
        assertNull(VehiclePersistenceMapper.toJpaEntity(null));
        assertNull(VehiclePersistenceMapper.toJpaReference(null));
    }
}
