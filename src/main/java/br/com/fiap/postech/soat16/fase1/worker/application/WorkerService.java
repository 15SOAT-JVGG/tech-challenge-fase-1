package br.com.fiap.postech.soat16.fase1.worker.application;

import static java.lang.Boolean.TRUE;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.shared.application.result.PagedResult;
import br.com.fiap.postech.soat16.fase1.worker.application.command.CreateWorkerCommand;
import br.com.fiap.postech.soat16.fase1.worker.application.command.LoginWorkerCommand;
import br.com.fiap.postech.soat16.fase1.worker.application.command.UpdateWorkerCommand;
import br.com.fiap.postech.soat16.fase1.worker.application.port.out.WorkerPasswordPort;
import br.com.fiap.postech.soat16.fase1.worker.application.port.out.WorkerPersistencePort;
import br.com.fiap.postech.soat16.fase1.worker.application.result.WorkerLoginResult;
import br.com.fiap.postech.soat16.fase1.worker.application.result.WorkerResult;
import br.com.fiap.postech.soat16.fase1.worker.domain.exception.DuplicateWorkerEmailException;
import br.com.fiap.postech.soat16.fase1.worker.domain.exception.InactiveWorkerException;
import br.com.fiap.postech.soat16.fase1.worker.domain.exception.InvalidWorkerCredentialsException;
import br.com.fiap.postech.soat16.fase1.worker.domain.exception.WorkerNotFoundException;
import br.com.fiap.postech.soat16.fase1.worker.domain.model.Worker;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class WorkerService {

    private final WorkerPersistencePort repository;
    private final WorkerPasswordPort password;

    @WithSession
    public Uni<PagedResult<WorkerResult>> findAll(String q, int page, int size) {
        return Uni.combine().all().unis(repository.findPage(page, size), repository.countWorkers()).asTuple()
                .map(tuple -> PagedResult.of(
                        tuple.getItem1().stream().map(WorkerResult::from).toList(),
                        page,
                        size,
                        tuple.getItem2()));
    }

    @WithSession
    public Uni<WorkerResult> findById(UUID id) {
        return repository.findByWorkerId(id)
                .onItem().ifNull().failWith(() -> new WorkerNotFoundException(id))
                .map(WorkerResult::from);
    }

    @WithTransaction
    public Uni<Void> create(CreateWorkerCommand command) {
        return repository.existsByEmail(command.email())
                .flatMap(exists -> {
                    if (TRUE.equals(exists)) {
                        return Uni.createFrom().<Void>failure(new DuplicateWorkerEmailException());
                    }
                    Worker worker = Worker.create(
                            command.profile(),
                            command.firstName(),
                            command.lastName(),
                            command.email(),
                            command.phoneNumber(),
                            password.hash(command.password()));
                    return repository.save(worker).replaceWithVoid();
                });
    }

    @WithTransaction
    public Uni<WorkerResult> update(UpdateWorkerCommand command) {
        return repository.findByWorkerId(command.id())
                .onItem().ifNull().failWith(() -> new WorkerNotFoundException(command.id()))
                .flatMap(worker -> repository.existsByEmailAndDifferentId(command.email(), command.id())
                        .flatMap(exists -> {
                            if (TRUE.equals(exists)) {
                                return Uni.createFrom().<Worker>failure(new DuplicateWorkerEmailException());
                            }
                            worker.update(
                                    command.profile(),
                                    command.firstName(),
                                    command.lastName(),
                                    command.email(),
                                    command.phoneNumber());
                            return repository.save(worker);
                        }))
                .map(WorkerResult::from);
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
    public Uni<WorkerLoginResult> login(LoginWorkerCommand command) {
        return repository.findByEmail(command.email())
                .onItem().ifNull().failWith(InvalidWorkerCredentialsException::new)
                .flatMap(worker -> {
                    if (!worker.isActive()) {
                        return Uni.createFrom().failure(new InactiveWorkerException());
                    }
                    if (!password.matches(command.password(), worker.getPasswordHash())) {
                        return Uni.createFrom().failure(new InvalidWorkerCredentialsException());
                    }
                    return Uni.createFrom().item(WorkerLoginResult.from(worker));
                });
    }
}
