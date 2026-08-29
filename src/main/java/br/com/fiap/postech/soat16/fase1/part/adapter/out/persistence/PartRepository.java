package br.com.fiap.postech.soat16.fase1.part.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.part.application.port.out.PartPersistencePort;
import br.com.fiap.postech.soat16.fase1.part.domain.model.Part;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class PartRepository implements PanacheRepositoryBase<Part, UUID>, PartPersistencePort {

    @Override
    public Uni<List<Part>> listAllParts() {
        return listAll();
    }

    @Override
    public Uni<Part> findPartById(UUID id) {
        return findById(id);
    }

    @Override
    public Uni<List<Part>> findLowStock() {
        return list("stockQuantity <= minimumStock");
    }

    @Override
    public Uni<Part> save(Part part) {
        return persist(part);
    }

    @Override
    public Uni<Boolean> deletePartById(UUID id) {
        return deleteById(id);
    }
}
