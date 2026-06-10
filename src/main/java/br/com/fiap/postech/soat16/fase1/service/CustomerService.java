package br.com.fiap.postech.soat16.fase1.service;

import static java.lang.Boolean.TRUE;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponse;
import br.com.fiap.postech.soat16.fase1.dto.request.CustomerCreateRequest;
import br.com.fiap.postech.soat16.fase1.dto.request.CustomerUpdateRequest;
import br.com.fiap.postech.soat16.fase1.dto.response.CustomerResponse;
import br.com.fiap.postech.soat16.fase1.exception.CustomerNotFoundException;
import br.com.fiap.postech.soat16.fase1.exception.DuplicatePhoneNumberException;
import br.com.fiap.postech.soat16.fase1.mapper.CustomerMapper;
import br.com.fiap.postech.soat16.fase1.repository.CustomerRepository;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    @WithSession
    public Uni<PageableResponse<CustomerResponse>> findAll(String q, int page, int size) {
        return Uni.combine().all()
                .unis(repository.findPage(page, size), repository.count())
                .asTuple()
                .map(tuple -> {
                    var data = tuple.getItem1().stream().map(mapper::toResponse).toList();
                    return PageableResponse.of(data, page, size, tuple.getItem2());
                });
    }

    @WithSession
    public Uni<CustomerResponse> findById(UUID id) {
        return repository.findByCustomerId(id)
                .onItem().ifNull().failWith(() -> new CustomerNotFoundException(id))
                .map(mapper::toResponse);
    }

    @WithTransaction
    public Uni<Void> create(CustomerCreateRequest request) {
        return repository.existsByPhoneNumber(request.getPhoneNumber())
                .flatMap(exists -> {
                    if (TRUE.equals(exists)) {
                        throw new DuplicatePhoneNumberException();
                    }
                    return repository.persist(mapper.toEntity(request)).replaceWithVoid();
                });
    }

    @WithTransaction
    public Uni<CustomerResponse> update(UUID id, CustomerUpdateRequest request) {
        return repository.findByCustomerId(id)
                .flatMap(entity -> {
                    if (entity == null) {
                        throw new CustomerNotFoundException(id);
                    }
                    mapper.updateEntity(entity, request);
                    return repository.persist(entity).map(mapper::toResponse);
                });
    }

    @WithTransaction
    public Uni<Void> delete(UUID id) {
        return repository.deleteByCustomerId(id)
                .flatMap(deleted -> {
                    if (deleted == 0) {
                        throw new CustomerNotFoundException(id);
                    }
                    return Uni.createFrom().voidItem();
                });
    }
}
