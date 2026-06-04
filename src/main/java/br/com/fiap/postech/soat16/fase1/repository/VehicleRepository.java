package br.com.fiap.postech.soat16.fase1.repository;

import br.com.fiap.postech.soat16.fase1.model.Vehicle;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class VehicleRepository implements PanacheRepository<Vehicle> {

    public Uni<List<Vehicle>> findPage(int page, int size) {
        return find("ORDER BY created_at DESC").page(page, size).list();
    }

    public Uni<Boolean> existsByLicensePlate(String licensePlate) {
        return count("license_plate = ?1", licensePlate).map(total -> total > 0);
    }
}
