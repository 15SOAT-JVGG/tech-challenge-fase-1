package br.com.fiap.postech.soat16.fase1.service;

import static java.lang.Boolean.FALSE;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.dto.request.ServiceItemRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.ServiceItemResponseDto;
import br.com.fiap.postech.soat16.fase1.exception.ResourceNotFoundException;
import br.com.fiap.postech.soat16.fase1.model.ServiceItem;
import br.com.fiap.postech.soat16.fase1.repository.ServiceItemRepository;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class ServiceItemService {

    private static final String SERVICE = "Service";

    private final ServiceItemRepository repository;

    public ServiceItemService(ServiceItemRepository repository) {
        this.repository = repository;
    }

    @WithSession
    public Uni<List<ServiceItemResponseDto>> listAll() {
        return repository.listAll()
            .map(items -> items.stream().map(ServiceItemResponseDto::from).toList());
    }

    @WithSession
    public Uni<ServiceItemResponseDto> findById(UUID id) {
        return repository.findById(id)
            .onItem().ifNull().failWith(() -> notFound(id))
            .map(ServiceItemResponseDto::from);
    }

    @WithTransaction
    public Uni<ServiceItemResponseDto> create(ServiceItemRequestDto dto) {
        var entity = new ServiceItem();
        apply(entity, dto);
        return repository.persist(entity).map(ServiceItemResponseDto::from);
    }

    @WithTransaction
    public Uni<ServiceItemResponseDto> update(UUID id, ServiceItemRequestDto dto) {
        return repository.findById(id)
            .onItem().ifNull().failWith(() -> notFound(id))
            .flatMap(entity -> {
                apply(entity, dto);
                return repository.persist(entity);
            })
            .map(ServiceItemResponseDto::from);
    }

    @WithTransaction
    public Uni<Void> delete(UUID id) {
        return repository.deleteById(id)
            .flatMap(deleted -> FALSE.equals(deleted)
                ? Uni.createFrom().failure(notFound(id))
                : Uni.createFrom().voidItem());
    }

    private void apply(ServiceItem entity, ServiceItemRequestDto dto) {
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setBasePrice(dto.basePrice());
        entity.setEstimatedDurationMinutes(dto.estimatedDurationMinutes());
        entity.setActive(dto.active() == null || dto.active());
    }

    private ResourceNotFoundException notFound(UUID id) {
        return new ResourceNotFoundException(SERVICE + " not found with id: " + id);
    }
}
