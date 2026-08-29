package br.com.fiap.postech.soat16.fase1.vehicle.application.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Consulta de veículos — testes unitários")
class VehicleQueryTest {

    @Test
    @DisplayName("usa createdAt descendente como ordenação padrão")
    void usesDefaultOrder() {
        VehicleQuery query = VehicleQuery.of(0, 10, List.of(), null, null, null);
        VehicleQuery queryWithNullSort =
                VehicleQuery.of(0, 10, null, null, null, null);

        assertEquals("createdAt", query.orders().getFirst().field());
        assertEquals(VehicleQuery.Direction.DESCENDING, query.orders().getFirst().direction());
        assertEquals(query.orders(), queryWithNullSort.orders());
    }

    @Test
    @DisplayName("interpreta ordenações explícitas e preserva todos os filtros")
    void parsesSortFields() {
        VehicleQuery query = VehicleQuery.of(
                1,
                20,
                List.of("manufacturer,asc", "model,desc"),
                "ABC",
                "Toyota",
                "Corolla");

        assertEquals(1, query.page());
        assertEquals(20, query.size());
        assertEquals(2, query.orders().size());
        assertEquals("manufacturer", query.orders().getFirst().field());
        assertEquals(VehicleQuery.Direction.ASCENDING, query.orders().getFirst().direction());
        assertEquals("model", query.orders().getLast().field());
        assertEquals(VehicleQuery.Direction.DESCENDING, query.orders().getLast().direction());
        assertEquals("ABC", query.licensePlate());
        assertEquals("Toyota", query.manufacturer());
        assertEquals("Corolla", query.model());
    }

    @Test
    @DisplayName("usa ordenação ascendente quando a direção é omitida ou desconhecida")
    void defaultsExplicitOrderToAscending() {
        VehicleQuery query = VehicleQuery.of(
                0,
                10,
                List.of("manufacturer", "model,invalid"),
                null,
                null,
                null);

        assertTrue(query.orders().stream()
                .allMatch(order ->
                        order.direction() == VehicleQuery.Direction.ASCENDING));
    }

    @Test
    @DisplayName("copia defensivamente a lista de ordens")
    void copiesOrdersDefensively() {
        List<VehicleQuery.Order> orders = new ArrayList<>();
        orders.add(new VehicleQuery.Order(
                "manufacturer", VehicleQuery.Direction.ASCENDING));

        VehicleQuery query =
                new VehicleQuery(0, 10, orders, null, null, null);
        orders.clear();

        assertEquals(1, query.orders().size());
        assertEquals("manufacturer", query.orders().getFirst().field());
    }
}
