package br.com.fiap.postech.soat16.fase1.servicecatalog.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.servicecatalog.application.port.out.ServiceCatalogPersistencePort;
import br.com.fiap.postech.soat16.fase1.servicecatalog.domain.model.ServiceItem;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class ServiceItemRepository
        implements PanacheRepositoryBase<ServiceItem, UUID>, ServiceCatalogPersistencePort {

    @Override
    public Uni<List<ServiceItem>> listAllServiceItems() {
        return listAll();
    }

    @Override
    public Uni<ServiceItem> findServiceItemById(UUID id) {
        return findById(id);
    }

    @Override
    public Uni<ServiceItem> save(ServiceItem serviceItem) {
        return persist(serviceItem);
    }

    @Override
    public Uni<Boolean> deleteServiceItemById(UUID id) {
        return deleteById(id);
    }
}
