package br.com.fiap.postech.soat16.fase1.service.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VehicleQuery — Unit Tests")
class VehicleQueryTest {

    @Test
    @DisplayName("uses createdAt descending as the default order")
    void usesDefaultOrder() {
        VehicleQuery query = VehicleQuery.of(0, 10, List.of(), null, null, null);

        assertEquals("createdAt", query.orders().getFirst().field());
        assertEquals(VehicleQuery.Direction.DESCENDING, query.orders().getFirst().direction());
    }

    @Test
    @DisplayName("parses explicit sort fields without transport dependencies")
    void parsesSortFields() {
        VehicleQuery query = VehicleQuery.of(
                1, 20, List.of("manufacturer,asc", "model,desc"),
                "ABC", "Toyota", "Corolla");

        assertEquals(2, query.orders().size());
        assertEquals(VehicleQuery.Direction.ASCENDING, query.orders().getFirst().direction());
        assertEquals(VehicleQuery.Direction.DESCENDING, query.orders().getLast().direction());
        assertEquals("Toyota", query.manufacturer());
    }
}
