package br.com.fiap.postech.soat16.fase1.repository;

import br.com.fiap.postech.soat16.fase1.model.Part;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class PartRepository implements PanacheRepository<Part> {

    /**
     * Peças cujo estoque atual está igual ou abaixo do mínimo definido para cada peça.
     */
    public Uni<List<Part>> findLowStock() {
        return list("stockQuantity <= minimumStock");
    }
}
