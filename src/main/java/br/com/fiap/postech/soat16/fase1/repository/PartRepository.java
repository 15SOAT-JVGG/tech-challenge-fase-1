package br.com.fiap.postech.soat16.fase1.repository;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.model.Part;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class PartRepository implements PanacheRepository<Part> {

    /**
     * Parts whose current stock is at or below the minimum defined for each part.
     */
    public Uni<List<Part>> findLowStock() {
        return list("stockQuantity <= minimumStock");
    }
}
