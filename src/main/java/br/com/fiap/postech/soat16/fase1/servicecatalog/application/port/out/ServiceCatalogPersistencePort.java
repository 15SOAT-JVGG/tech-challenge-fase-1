package br.com.fiap.postech.soat16.fase1.servicecatalog.application.port.out;

import java.util.List;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.servicecatalog.domain.model.ServiceItem;

import io.smallrye.mutiny.Uni;

public interface ServiceCatalogPersistencePort {

    Uni<List<ServiceItem>> listAllServiceItems();

    Uni<ServiceItem> findServiceItemById(UUID id);

    Uni<ServiceItem> save(ServiceItem serviceItem);

    Uni<Boolean> deleteServiceItemById(UUID id);
}
