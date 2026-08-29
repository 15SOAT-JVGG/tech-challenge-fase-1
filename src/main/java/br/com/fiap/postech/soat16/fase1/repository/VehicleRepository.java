package br.com.fiap.postech.soat16.fase1.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.service.query.VehicleQuery;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class VehicleRepository implements PanacheRepository<Vehicle> {

    public Uni<Vehicle> findByLicensePlate(String licensePlate) {
        return find("licensePlate = ?1", licensePlate).firstResult();
    }

    public Uni<Boolean> existsByLicensePlate(String licensePlate) {
        return count("licensePlate = ?1", licensePlate).map(total -> total > 0);
    }

    public Uni<Boolean> existsByCustomerId(UUID customerId) {
        return count("customer.id = ?1", customerId).map(total -> total > 0);
    }

    public Uni<Vehicle> findByVehicleId(UUID id) {
        return find("id = ?1", id).firstResult()
                .invoke(found -> Log.infof("Vehicle lookup: id=%s found=%b", id, found != null));
    }

    public Uni<Long> deleteByVehicleId(UUID id) {
        return delete("id = ?1", id)
                .invoke(deleted -> Log.infof("Vehicle deleted: id=%s deleted=%d", id, deleted));
    }

    public Uni<List<Vehicle>> findPageWithFilter(VehicleQuery query) {
        var filterResult = buildFilter(query);
        Sort sort = buildSort(query);
        if (filterResult.query().isEmpty()) {
            return findAll(sort)
                    .page(query.page(), query.size())
                    .list();
        }
        return find(filterResult.query(), sort, filterResult.params())
                .page(query.page(), query.size())
                .list();
    }

    public Uni<Long> countWithFilter(VehicleQuery query) {
        var filterResult = buildFilter(query);
        if (filterResult.query().isEmpty()) {
            return count();
        }
        return count(filterResult.query(), filterResult.params());
    }

    private FilterResult buildFilter(VehicleQuery query) {

        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (query.licensePlate() != null && !query.licensePlate().isBlank()) {
            conditions.add("LOWER(licensePlate) LIKE :licensePlate");
            params.put("licensePlate", "%" + query.licensePlate().toLowerCase(Locale.ROOT) + "%");
        }

        if (query.manufacturer() != null && !query.manufacturer().isBlank()) {
            conditions.add("LOWER(manufacturer) LIKE :manufacturer");
            params.put("manufacturer", "%" + query.manufacturer().toLowerCase(Locale.ROOT) + "%");
        }

        if (query.model() != null && !query.model().isBlank()) {
            conditions.add("LOWER(model) LIKE :model");
            params.put("model", "%" + query.model().toLowerCase(Locale.ROOT) + "%");
        }

        return new FilterResult(String.join(" AND ", conditions), params);
    }

    private Sort buildSort(VehicleQuery query) {
        List<VehicleQuery.Order> orders = query.orders().isEmpty()
                ? List.of(new VehicleQuery.Order("createdAt", VehicleQuery.Direction.DESCENDING))
                : query.orders();
        Sort result = null;
        for (VehicleQuery.Order order : orders) {
            Sort.Direction direction = order.direction() == VehicleQuery.Direction.DESCENDING
                    ? Sort.Direction.Descending
                    : Sort.Direction.Ascending;
            result = result == null ? Sort.by(order.field(), direction) : result.and(order.field(), direction);
        }
        return result;
    }

    private record FilterResult(String query, Map<String, Object> params) { }
}
