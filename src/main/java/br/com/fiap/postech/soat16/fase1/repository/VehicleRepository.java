package br.com.fiap.postech.soat16.fase1.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.VehicleFilterDto;
import br.com.fiap.postech.soat16.fase1.model.Vehicle;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class VehicleRepository implements PanacheRepository<Vehicle> {

    public Uni<Vehicle> findByLicensePlate(String licensePlate) {
        return find("licensePlate = ?1", licensePlate).firstResult();
    }

    public Uni<Boolean> existsByLicensePlate(String licensePlate) {
        return count("licensePlate = ?1", licensePlate).map(total -> total > 0);
    }

    public Uni<List<Vehicle>> findPageWithFilter(PageableRequestDto pageable, VehicleFilterDto filter) {
        var filterResult = buildFilter(filter);
        if (filterResult.query().isEmpty()) {
            return findAll(pageable.getSort())
                    .page(pageable.getPage(), pageable.getSize())
                    .list();
        }
        return find(filterResult.query(), pageable.getSort(), filterResult.params())
                .page(pageable.getPage(), pageable.getSize())
                .list();
    }

    public Uni<Long> countWithFilter(VehicleFilterDto filter) {
        var filterResult = buildFilter(filter);
        if (filterResult.query().isEmpty()) {
            return count();
        }
        return count(filterResult.query(), filterResult.params());
    }

    private FilterResult buildFilter(VehicleFilterDto filter) {

        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (filter.getLicensePlate() != null && !filter.getLicensePlate().isBlank()) {
            conditions.add("LOWER(licensePlate) LIKE :licensePlate");
            params.put("licensePlate", "%" + filter.getLicensePlate().toLowerCase() + "%");
        }

        if (filter.getManufacturer() != null && !filter.getManufacturer().isBlank()) {
            conditions.add("LOWER(manufacturer) LIKE :manufacturer");
            params.put("manufacturer", "%" + filter.getManufacturer().toLowerCase() + "%");
        }

        if (filter.getModel() != null && !filter.getModel().isBlank()) {
            conditions.add("LOWER(model) LIKE :model");
            params.put("model", "%" + filter.getModel().toLowerCase() + "%");
        }

        return new FilterResult(String.join(" AND ", conditions), params);
    }

    private record FilterResult(String query, Map<String, Object> params) {}
}
