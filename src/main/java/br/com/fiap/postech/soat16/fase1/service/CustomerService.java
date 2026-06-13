package br.com.fiap.postech.soat16.fase1.service;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponse;
import br.com.fiap.postech.soat16.fase1.dto.request.CustomerCreateRequest;
import br.com.fiap.postech.soat16.fase1.dto.request.CustomerUpdateRequest;
import br.com.fiap.postech.soat16.fase1.dto.response.CustomerResponse;
import br.com.fiap.postech.soat16.fase1.exception.CustomerNotFoundException;
import br.com.fiap.postech.soat16.fase1.exception.DuplicateDocumentException;
import br.com.fiap.postech.soat16.fase1.mapper.CustomerMapper;
import br.com.fiap.postech.soat16.fase1.model.Document;
import br.com.fiap.postech.soat16.fase1.repository.CustomerRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import static java.lang.Boolean.TRUE;

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
        Document document = Document.of(request.getDocument());

        return repository.existsByDocument(document.getValue())
                .flatMap(docExists -> {
                    if (TRUE.equals(docExists)) throw new DuplicateDocumentException();
                    return repository.persist(mapper.toEntity(request, document)).replaceWithVoid();
                });
    }

    @WithSession
    public Uni<CustomerResponse> findByDocument(String rawDocument) {
        Document document = Document.of(rawDocument);
        return repository.findByDocument(document.getValue())
                .onItem().ifNull().failWith(() -> new CustomerNotFoundException(rawDocument))
                .map(mapper::toResponse);
    }

    @WithTransaction
    public Uni<CustomerResponse> update(UUID id, CustomerUpdateRequest request) {
        return repository.findByCustomerId(id)
                .onItem().ifNull().failWith(() -> new CustomerNotFoundException(id))
                .flatMap(entity -> {
                    mapper.updateEntity(entity, request);
                    return repository.persist(entity).map(mapper::toResponse);
                });
    }

    @WithTransaction
    public Uni<Void> delete(UUID id) {
        return repository.deleteByCustomerId(id)
                .flatMap(deleted -> {
                    if (deleted == 0) throw new CustomerNotFoundException(id);
                    return Uni.createFrom().voidItem();
                });
    }
}
