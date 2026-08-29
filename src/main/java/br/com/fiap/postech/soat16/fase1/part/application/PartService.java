package br.com.fiap.postech.soat16.fase1.part.application;

import static java.lang.Boolean.FALSE;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.part.application.command.AdjustPartStockCommand;
import br.com.fiap.postech.soat16.fase1.part.application.command.CreatePartCommand;
import br.com.fiap.postech.soat16.fase1.part.application.command.DeletePartCommand;
import br.com.fiap.postech.soat16.fase1.part.application.command.FindPartQuery;
import br.com.fiap.postech.soat16.fase1.part.application.command.UpdatePartCommand;
import br.com.fiap.postech.soat16.fase1.part.application.port.out.PartPersistencePort;
import br.com.fiap.postech.soat16.fase1.part.application.result.PartResult;
import br.com.fiap.postech.soat16.fase1.part.domain.model.Part;
import br.com.fiap.postech.soat16.fase1.shared.domain.exception.ResourceNotFoundException;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class PartService {

    public static final String PECA_INSUMO = "Part/Supply";

    private final PartPersistencePort partPersistence;

    @WithSession
    public Uni<List<PartResult>> listAll() {
        return partPersistence.listAllParts()
                .map(parts -> parts.stream().map(PartResult::from).toList());
    }

    @WithSession
    public Uni<PartResult> findById(FindPartQuery query) {
        return partPersistence.findPartById(query.id())
                .onItem().ifNull().failWith(() -> notFound(query.id()))
                .map(PartResult::from);
    }

    @WithSession
    public Uni<List<PartResult>> findLowStock() {
        return partPersistence.findLowStock()
                .map(parts -> parts.stream().map(PartResult::from).toList());
    }

    @WithTransaction
    public Uni<PartResult> create(CreatePartCommand command) {
        Part part = new Part(
                command.name(),
                command.description(),
                command.unitPrice(),
                command.stockQuantity(),
                command.unit(),
                command.minimumStock(),
                command.partType());
        return partPersistence.save(part).map(PartResult::from);
    }

    @WithTransaction
    public Uni<PartResult> update(UpdatePartCommand command) {
        return partPersistence.findPartById(command.id())
                .onItem().ifNull().failWith(() -> notFound(command.id()))
                .flatMap(part -> {
                    part.update(
                            command.name(),
                            command.description(),
                            command.unitPrice(),
                            command.stockQuantity(),
                            command.unit(),
                            command.minimumStock(),
                            command.partType());
                    return partPersistence.save(part);
                })
                .map(PartResult::from);
    }

    @WithTransaction
    public Uni<PartResult> adjustStock(AdjustPartStockCommand command) {
        return partPersistence.findPartById(command.id())
                .onItem().ifNull().failWith(() -> notFound(command.id()))
                .flatMap(part -> {
                    if (command.adjustment() > 0) {
                        part.increaseStock(command.adjustment());
                    } else {
                        part.decreaseStock(Math.abs(command.adjustment()));
                    }
                    return partPersistence.save(part);
                })
                .map(PartResult::from);
    }

    @WithTransaction
    public Uni<Void> delete(DeletePartCommand command) {
        return partPersistence.deletePartById(command.id())
                .flatMap(deleted -> {
                    if (FALSE.equals(deleted)) {
                        return Uni.createFrom().failure(notFound(command.id()));
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    private ResourceNotFoundException notFound(Object id) {
        return new ResourceNotFoundException(PECA_INSUMO, id);
    }
}
