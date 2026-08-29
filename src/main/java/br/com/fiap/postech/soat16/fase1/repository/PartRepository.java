package br.com.fiap.postech.soat16.fase1.repository;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.model.Part;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class PartRepository implements PanacheRepositoryBase<Part, UUID> {

    public Uni<List<Part>> findLowStock() {
        return list("stockQuantity <= minimumStock");
    }
}
