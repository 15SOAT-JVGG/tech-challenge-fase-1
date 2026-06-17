package br.com.fiap.postech.soat16.fase1.service;

import static java.lang.Boolean.TRUE;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.request.CustomerRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.CustomerResponseDto;
import br.com.fiap.postech.soat16.fase1.exception.CustomerHasVehiclesException;
import br.com.fiap.postech.soat16.fase1.exception.CustomerNotFoundException;
import br.com.fiap.postech.soat16.fase1.exception.DuplicateDocumentException;
import br.com.fiap.postech.soat16.fase1.mapper.CustomerMapper;
import br.com.fiap.postech.soat16.fase1.model.Document;
import br.com.fiap.postech.soat16.fase1.repository.CustomerRepository;
import br.com.fiap.postech.soat16.fase1.repository.VehicleRepository;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;
    private final VehicleRepository vehicleRepository;
    private final CustomerMapper mapper;

    @WithSession
    public Uni<PageableResponseDto<CustomerResponseDto>> findAll(String q, int page, int size) {
        return Uni.combine().all()
                .unis(repository.findPage(page, size), repository.count())
                .asTuple()
                .map(tuple -> {
                    var data = tuple.getItem1().stream().map(mapper::toResponse).toList();
                    return PageableResponseDto.of(data, page, size, tuple.getItem2());
                });
    }

    @WithSession
    public Uni<CustomerResponseDto> findById(UUID id) {
        return repository.findByCustomerId(id)
                .onItem().ifNull().failWith(CustomerNotFoundException::new)
                .map(mapper::toResponse);
    }

    @WithTransaction
    public Uni<Void> create(CustomerRequestDto request) {
        Document document = Document.of(request.document());

        return repository.existsByDocument(document.getValue())
                .flatMap(docExists -> {
                    if (TRUE.equals(docExists)) {
                        return Uni.createFrom().<Void>failure(new DuplicateDocumentException());
                    }
                    return repository.persist(mapper.toEntity(request, document)).replaceWithVoid();
                });
    }

    @WithSession
    public Uni<CustomerResponseDto> findByDocument(String rawDocument) {
        Document document = Document.of(rawDocument);
        return repository.findByDocument(document.getValue())
                .onItem().ifNull().failWith(() -> new CustomerNotFoundException(rawDocument))
                .map(mapper::toResponse);
    }

    @WithTransaction
    public Uni<CustomerResponseDto> update(UUID id, CustomerRequestDto request) {
        return repository.findByCustomerId(id)
                .onItem().ifNull().failWith(CustomerNotFoundException::new)
                .flatMap(entity -> {
                    mapper.updateEntity(entity, request);
                    return repository.persist(entity).map(mapper::toResponse);
                });
    }

    @WithTransaction
    public Uni<Void> delete(UUID id) {
        return vehicleRepository.existsByCustomerId(id)
                .flatMap(hasVehicles -> {
                    if (TRUE.equals(hasVehicles)) {
                        return Uni.createFrom().<Long>failure(new CustomerHasVehiclesException());
                    }
                    return repository.deleteByCustomerId(id);
                })
                .flatMap(deleted -> {
                    if (deleted == 0) {
                        return Uni.createFrom().<Void>failure(new CustomerNotFoundException());
                    }
                    return Uni.createFrom().voidItem();
                });
    }
}
