package br.com.fiap.postech.soat16.fase1.vehicle.adapter.out.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.vehicle.adapter.out.persistence.entity.VehicleJpaEntity;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.out.persistence.mapper.VehiclePersistenceMapper;
import br.com.fiap.postech.soat16.fase1.vehicle.application.port.out.VehiclePersistencePort;
import br.com.fiap.postech.soat16.fase1.vehicle.application.query.VehicleQuery;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.Vehicle;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class VehicleRepository
        implements PanacheRepositoryBase<VehicleJpaEntity, UUID>, VehiclePersistencePort {

    @Override
    public Uni<Vehicle> findByLicensePlate(String licensePlate) {
        return find("licensePlate = ?1", licensePlate).firstResult()
                .map(VehiclePersistenceMapper::toDomain);
    }

    @Override
    public Uni<Boolean> existsByLicensePlate(String licensePlate) {
        return count("licensePlate = ?1", licensePlate).map(total -> total > 0);
    }

    @Override
    public Uni<Boolean> existsByCustomerId(UUID customerId) {
        return count("customer.id = ?1", customerId).map(total -> total > 0);
    }

    @Override
    public Uni<Vehicle> findByVehicleId(UUID id) {
        return findById(id)
                .invoke(found -> Log.infof("Vehicle lookup: id=%s found=%b", id, found != null))
                .map(VehiclePersistenceMapper::toDomain);
    }

    @Override
    public Uni<Long> deleteByVehicleId(UUID id) {
        return delete("id = ?1", id)
                .invoke(deleted -> Log.infof("Vehicle deleted: id=%s deleted=%d", id, deleted));
    }

    @Override
    public Uni<List<Vehicle>> findPageWithFilter(VehicleQuery query) {
        FilterResult filterResult = buildFilter(query);
        Sort sort = buildSort(query);
        Uni<List<VehicleJpaEntity>> entities = filterResult.query().isEmpty()
                ? findAll(sort).page(query.page(), query.size()).list()
                : find(filterResult.query(), sort, filterResult.params())
                        .page(query.page(), query.size())
                        .list();
        return entities.map(list -> list.stream()
                .map(VehiclePersistenceMapper::toDomain)
                .toList());
    }

    @Override
    public Uni<Long> countWithFilter(VehicleQuery query) {
        FilterResult filterResult = buildFilter(query);
        if (filterResult.query().isEmpty()) {
            return count();
        }
        return count(filterResult.query(), filterResult.params());
    }

    @Override
    public Uni<Vehicle> save(Vehicle vehicle) {
        return upsert(vehicle).replaceWith(vehicle);
    }

    private Uni<VehicleJpaEntity> upsert(Vehicle vehicle) {
        if (vehicle.getId() == null) {
            return persist(VehiclePersistenceMapper.toJpaEntity(vehicle))
                    .invoke(entity -> VehiclePersistenceMapper.copyGeneratedState(entity, vehicle));
        }
        return findById(vehicle.getId())
                .onItem().ifNotNull()
                .invoke(entity -> VehiclePersistenceMapper.copyState(vehicle, entity));
    }

    private FilterResult buildFilter(VehicleQuery query) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (query.licensePlate() != null && !query.licensePlate().isBlank()) {
            conditions.add("LOWER(licensePlate) LIKE :licensePlate");
            params.put(
                    "licensePlate",
                    "%" + query.licensePlate().toLowerCase(Locale.ROOT) + "%");
        }

        if (query.manufacturer() != null && !query.manufacturer().isBlank()) {
            conditions.add("LOWER(manufacturer) LIKE :manufacturer");
            params.put(
                    "manufacturer",
                    "%" + query.manufacturer().toLowerCase(Locale.ROOT) + "%");
        }

        if (query.model() != null && !query.model().isBlank()) {
            conditions.add("LOWER(model) LIKE :model");
            params.put("model", "%" + query.model().toLowerCase(Locale.ROOT) + "%");
        }

        return new FilterResult(String.join(" AND ", conditions), params);
    }

    private Sort buildSort(VehicleQuery query) {
        List<VehicleQuery.Order> orders = query.orders().isEmpty()
                ? List.of(new VehicleQuery.Order(
                        "createdAt", VehicleQuery.Direction.DESCENDING))
                : query.orders();
        Sort result = null;
        for (VehicleQuery.Order order : orders) {
            Sort.Direction direction = order.direction() == VehicleQuery.Direction.DESCENDING
                    ? Sort.Direction.Descending
                    : Sort.Direction.Ascending;
            result = result == null
                    ? Sort.by(order.field(), direction)
                    : result.and(order.field(), direction);
        }
        return result;
    }

    private record FilterResult(String query, Map<String, Object> params) { }
}
