package br.com.fiap.postech.soat16.fase1.servicecatalog.application;

import static java.lang.Boolean.FALSE;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.servicecatalog.application.command.CreateServiceItemCommand;
import br.com.fiap.postech.soat16.fase1.servicecatalog.application.command.DeleteServiceItemCommand;
import br.com.fiap.postech.soat16.fase1.servicecatalog.application.command.FindServiceItemQuery;
import br.com.fiap.postech.soat16.fase1.servicecatalog.application.command.UpdateServiceItemCommand;
import br.com.fiap.postech.soat16.fase1.servicecatalog.application.port.out.ServiceCatalogPersistencePort;
import br.com.fiap.postech.soat16.fase1.servicecatalog.application.result.ServiceItemResult;
import br.com.fiap.postech.soat16.fase1.servicecatalog.domain.model.ServiceItem;
import br.com.fiap.postech.soat16.fase1.shared.domain.exception.ResourceNotFoundException;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class ServiceItemService {

    private static final String SERVICE = "Service";

    private final ServiceCatalogPersistencePort serviceCatalogPersistence;

    @WithSession
    public Uni<List<ServiceItemResult>> listAll() {
        return serviceCatalogPersistence.listAllServiceItems()
                .map(items -> items.stream().map(ServiceItemResult::from).toList());
    }

    @WithSession
    public Uni<ServiceItemResult> findById(FindServiceItemQuery query) {
        return serviceCatalogPersistence.findServiceItemById(query.id())
                .onItem().ifNull().failWith(() -> notFound(query.id()))
                .map(ServiceItemResult::from);
    }

    @WithTransaction
    public Uni<ServiceItemResult> create(CreateServiceItemCommand command) {
        var entity = new ServiceItem();
        apply(
                entity,
                command.name(),
                command.description(),
                command.basePrice(),
                command.estimatedDurationMinutes(),
                command.active());
        return serviceCatalogPersistence.save(entity).map(ServiceItemResult::from);
    }

    @WithTransaction
    public Uni<ServiceItemResult> update(UpdateServiceItemCommand command) {
        return serviceCatalogPersistence.findServiceItemById(command.id())
                .onItem().ifNull().failWith(() -> notFound(command.id()))
                .flatMap(entity -> {
                    apply(
                            entity,
                            command.name(),
                            command.description(),
                            command.basePrice(),
                            command.estimatedDurationMinutes(),
                            command.active());
                    return serviceCatalogPersistence.save(entity);
                })
                .map(ServiceItemResult::from);
    }

    @WithTransaction
    public Uni<Void> delete(DeleteServiceItemCommand command) {
        return serviceCatalogPersistence.deleteServiceItemById(command.id())
                .flatMap(deleted -> FALSE.equals(deleted)
                        ? Uni.createFrom().failure(notFound(command.id()))
                        : Uni.createFrom().voidItem());
    }

    private void apply(
            ServiceItem entity,
            String name,
            String description,
            BigDecimal basePrice,
            Integer estimatedDurationMinutes,
            Boolean active) {
        entity.setName(name);
        entity.setDescription(description);
        entity.setBasePrice(basePrice);
        entity.setEstimatedDurationMinutes(estimatedDurationMinutes);
        entity.setActive(active == null || active);
    }

    private ResourceNotFoundException notFound(UUID id) {
        return new ResourceNotFoundException(SERVICE + " not found with id: " + id);
    }
}
