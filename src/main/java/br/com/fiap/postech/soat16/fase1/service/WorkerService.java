package br.com.fiap.postech.soat16.fase1.service;

import static java.lang.Boolean.TRUE;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.pagination.ReactivePage;
import br.com.fiap.postech.soat16.fase1.dto.request.WorkerLoginRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.WorkerRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkerLoginResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkerResponseDto;
import br.com.fiap.postech.soat16.fase1.exception.DuplicateWorkerEmailException;
import br.com.fiap.postech.soat16.fase1.exception.InactiveWorkerException;
import br.com.fiap.postech.soat16.fase1.exception.InvalidWorkerCredentialsException;
import br.com.fiap.postech.soat16.fase1.exception.WorkerNotFoundException;
import br.com.fiap.postech.soat16.fase1.mapper.WorkerMapper;
import br.com.fiap.postech.soat16.fase1.repository.WorkerRepository;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class WorkerService {

    private final WorkerRepository repository;
    private final WorkerMapper mapper;
    private final PasswordService passwordService;

    @WithSession
    public Uni<PageableResponseDto<WorkerResponseDto>> findAll(String q, int page, int size) {
        return ReactivePage.of(repository.findPage(page, size), repository.count(), mapper::toResponse, page, size);
    }

    @WithSession
    public Uni<WorkerResponseDto> findById(UUID id) {
        return repository.findByWorkerId(id)
                .onItem().ifNull().failWith(() -> new WorkerNotFoundException(id))
                .map(mapper::toResponse);
    }

    @WithTransaction
    public Uni<Void> create(WorkerRequestDto request) {
        return repository.existsByEmail(request.email())
                .flatMap(exists -> {
                    if (TRUE.equals(exists)) {
                        return Uni.createFrom().<Void>failure(new DuplicateWorkerEmailException());
                    }
                    String passwordHash = passwordService.hash(request.password());
                    return repository.persist(mapper.toEntity(request, passwordHash)).replaceWithVoid();
                });
    }

    @WithTransaction
    public Uni<WorkerResponseDto> update(UUID id, WorkerRequestDto request) {
        return repository.findByWorkerId(id)
                .onItem().ifNull().failWith(() -> new WorkerNotFoundException(id))
                .flatMap(entity -> repository.existsByEmailAndDifferentId(request.email(), id)
                        .flatMap(exists -> {
                            if (TRUE.equals(exists)) {
                                return Uni.createFrom().<WorkerResponseDto>failure(new DuplicateWorkerEmailException());
                            }
                            mapper.updateEntity(entity, request);
                            return repository.persist(entity).map(mapper::toResponse);
                        }));
    }

    @WithTransaction
    public Uni<Void> delete(UUID id) {
        return repository.deleteByWorkerId(id)
                .flatMap(deleted -> {
                    if (deleted == 0) {
                        return Uni.createFrom().<Void>failure(new WorkerNotFoundException(id));
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    @WithSession
    public Uni<WorkerLoginResponseDto> login(WorkerLoginRequestDto request) {
        return repository.findByEmail(request.email())
                .onItem().ifNull().failWith(InvalidWorkerCredentialsException::new)
                .flatMap(entity -> {
                    if (!entity.isActive()) {
                        return Uni.createFrom().failure(new InactiveWorkerException());
                    }
                    if (!passwordService.matches(request.password(), entity.getPasswordHash())) {
                        return Uni.createFrom().failure(new InvalidWorkerCredentialsException());
                    }
                    return Uni.createFrom().item(mapper.toLoginResponse(entity));
                });
    }
}
