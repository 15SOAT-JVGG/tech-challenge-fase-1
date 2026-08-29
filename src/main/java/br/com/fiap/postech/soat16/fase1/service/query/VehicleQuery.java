package br.com.fiap.postech.soat16.fase1.service.query;

import java.util.List;

public record VehicleQuery(
        int page,
        int size,
        List<Order> orders,
        String licensePlate,
        String manufacturer,
        String model
) {

    public VehicleQuery {
        orders = orders == null ? List.of() : List.copyOf(orders);
    }

    public static VehicleQuery of(int page, int size, List<String> sortParameters,
            String licensePlate, String manufacturer, String model) {
        List<Order> orders = sortParameters == null || sortParameters.isEmpty()
                ? List.of(new Order("createdAt", Direction.DESCENDING))
                : sortParameters.stream().map(VehicleQuery::parseOrder).toList();
        return new VehicleQuery(page, size, orders, licensePlate, manufacturer, model);
    }

    private static Order parseOrder(String value) {
        String[] parts = value.split(",", 2);
        String field = parts[0].trim();
        Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Direction.DESCENDING
                : Direction.ASCENDING;
        return new Order(field, direction);
    }

    public record Order(String field, Direction direction) { }

    public enum Direction {
        ASCENDING,
        DESCENDING
    }
}
