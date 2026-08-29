package br.com.fiap.postech.soat16.fase1.part.application.port.out;

import java.util.List;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.part.domain.model.Part;

import io.smallrye.mutiny.Uni;

public interface PartPersistencePort {

    Uni<List<Part>> listAllParts();

    Uni<Part> findPartById(UUID id);

    Uni<List<Part>> findLowStock();

    Uni<Part> save(Part part);

    Uni<Boolean> deletePartById(UUID id);
}
