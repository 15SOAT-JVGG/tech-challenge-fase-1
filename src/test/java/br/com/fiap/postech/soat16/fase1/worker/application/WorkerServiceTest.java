package br.com.fiap.postech.soat16.fase1.worker.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.postech.soat16.fase1.worker.application.command.CreateWorkerCommand;
import br.com.fiap.postech.soat16.fase1.worker.application.command.LoginWorkerCommand;
import br.com.fiap.postech.soat16.fase1.worker.application.command.UpdateWorkerCommand;
import br.com.fiap.postech.soat16.fase1.worker.application.port.out.WorkerPasswordPort;
import br.com.fiap.postech.soat16.fase1.worker.application.port.out.WorkerPersistencePort;
import br.com.fiap.postech.soat16.fase1.worker.domain.exception.DuplicateWorkerEmailException;
import br.com.fiap.postech.soat16.fase1.worker.domain.exception.InactiveWorkerException;
import br.com.fiap.postech.soat16.fase1.worker.domain.exception.InvalidWorkerCredentialsException;
import br.com.fiap.postech.soat16.fase1.worker.domain.exception.WorkerNotFoundException;
import br.com.fiap.postech.soat16.fase1.worker.domain.model.Worker;
import br.com.fiap.postech.soat16.fase1.worker.domain.model.enums.WorkerProfile;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkerService — Unit Tests")
class WorkerServiceTest {

    private static final UUID WORKER_ID =
            UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @Mock
    WorkerPersistencePort repository;

    @Mock
    WorkerPasswordPort password;

    private WorkerService service;
    private Worker worker;

    @BeforeEach
    void setUp() {
        service = new WorkerService(repository, password);
        worker = new Worker(
                WORKER_ID,
                WorkerProfile.MECHANIC,
                "Ana",
                "Silva",
                "ana@example.com",
                "5511999999999",
                "hash",
                true);
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("returns application-owned pagination and preserves ignored q behavior")
        void returnsPagedResult() {
            when(repository.findPage(0, 10)).thenReturn(Uni.createFrom().item(List.of(worker)));
            when(repository.countWorkers()).thenReturn(Uni.createFrom().item(1L));

            var result = service.findAll("ignored", 0, 10).await().indefinitely();

            assertEquals(1, result.content().size());
            assertEquals(1L, result.totalElements());
            assertEquals(WORKER_ID, result.content().getFirst().workerId());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns the worker when found")
        void returnsWorker() {
            when(repository.findByWorkerId(WORKER_ID)).thenReturn(Uni.createFrom().item(worker));

            var result = service.findById(WORKER_ID).await().indefinitely();

            assertEquals(WORKER_ID, result.workerId());
        }

        @Test
        @DisplayName("reports a missing worker")
        void reportsMissingWorker() {
            when(repository.findByWorkerId(WORKER_ID)).thenReturn(Uni.createFrom().nullItem());

            assertThrows(
                    WorkerNotFoundException.class,
                    () -> service.findById(WORKER_ID).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("hashes the password and persists an active worker")
        void createsWorker() {
            CreateWorkerCommand command = createCommand();
            when(repository.existsByEmail(command.email())).thenReturn(Uni.createFrom().item(false));
            when(password.hash(command.password())).thenReturn("hash");
            when(repository.save(any(Worker.class)))
                    .thenAnswer(invocation -> Uni.createFrom().item(
                            (Worker) invocation.getArgument(0)));

            assertDoesNotThrow(() -> service.create(command).await().indefinitely());

            ArgumentCaptor<Worker> captor = ArgumentCaptor.forClass(Worker.class);
            verify(repository).save(captor.capture());
            assertEquals("hash", captor.getValue().getPasswordHash());
            assertEquals(WorkerProfile.MECHANIC, captor.getValue().getProfile());
            assertEquals(true, captor.getValue().isActive());
        }

        @Test
        @DisplayName("rejects duplicate email")
        void rejectsDuplicateEmail() {
            CreateWorkerCommand command = createCommand();
            when(repository.existsByEmail(command.email())).thenReturn(Uni.createFrom().item(true));

            assertThrows(
                    DuplicateWorkerEmailException.class,
                    () -> service.create(command).await().indefinitely());
            verify(repository, never()).save(any(Worker.class));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("updates mutable worker data without changing the password hash")
        void updatesWorker() {
            UpdateWorkerCommand command = updateCommand();
            when(repository.findByWorkerId(WORKER_ID)).thenReturn(Uni.createFrom().item(worker));
            when(repository.existsByEmailAndDifferentId(command.email(), WORKER_ID))
                    .thenReturn(Uni.createFrom().item(false));
            when(repository.save(worker)).thenReturn(Uni.createFrom().item(worker));

            var result = service.update(command).await().indefinitely();

            assertEquals("Maria", result.firstName());
            assertEquals("hash", worker.getPasswordHash());
            verify(repository).save(worker);
        }

        @Test
        @DisplayName("rejects an email owned by another worker")
        void rejectsDuplicateEmail() {
            UpdateWorkerCommand command = updateCommand();
            when(repository.findByWorkerId(WORKER_ID)).thenReturn(Uni.createFrom().item(worker));
            when(repository.existsByEmailAndDifferentId(command.email(), WORKER_ID))
                    .thenReturn(Uni.createFrom().item(true));

            assertThrows(
                    DuplicateWorkerEmailException.class,
                    () -> service.update(command).await().indefinitely());
            verify(repository, never()).save(any(Worker.class));
        }

        @Test
        @DisplayName("reports a missing worker")
        void reportsMissingWorker() {
            when(repository.findByWorkerId(WORKER_ID)).thenReturn(Uni.createFrom().nullItem());

            assertThrows(
                    WorkerNotFoundException.class,
                    () -> service.update(updateCommand()).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deletes an existing worker")
        void deletesWorker() {
            when(repository.deleteByWorkerId(WORKER_ID)).thenReturn(Uni.createFrom().item(1L));

            assertDoesNotThrow(() -> service.delete(WORKER_ID).await().indefinitely());
        }

        @Test
        @DisplayName("reports a missing worker")
        void reportsMissingWorker() {
            when(repository.deleteByWorkerId(WORKER_ID)).thenReturn(Uni.createFrom().item(0L));

            assertThrows(
                    WorkerNotFoundException.class,
                    () -> service.delete(WORKER_ID).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        private final LoginWorkerCommand command =
                new LoginWorkerCommand("ana@example.com", "password123");

        @Test
        @DisplayName("authenticates an active worker with a matching password")
        void authenticatesWorker() {
            when(repository.findByEmail(command.email())).thenReturn(Uni.createFrom().item(worker));
            when(password.matches(command.password(), worker.getPasswordHash())).thenReturn(true);

            var result = service.login(command).await().indefinitely();

            assertNotNull(result);
            assertEquals(WORKER_ID, result.workerId());
            assertEquals(true, result.authenticated());
        }

        @Test
        @DisplayName("rejects an unknown email")
        void rejectsUnknownEmail() {
            when(repository.findByEmail(command.email())).thenReturn(Uni.createFrom().nullItem());

            assertThrows(
                    InvalidWorkerCredentialsException.class,
                    () -> service.login(command).await().indefinitely());
        }

        @Test
        @DisplayName("rejects an inactive worker before checking the password")
        void rejectsInactiveWorker() {
            worker.setActive(false);
            when(repository.findByEmail(command.email())).thenReturn(Uni.createFrom().item(worker));

            assertThrows(
                    InactiveWorkerException.class,
                    () -> service.login(command).await().indefinitely());
            verify(password, never()).matches(any(), any());
        }

        @Test
        @DisplayName("rejects an invalid password")
        void rejectsInvalidPassword() {
            when(repository.findByEmail(command.email())).thenReturn(Uni.createFrom().item(worker));
            when(password.matches(command.password(), worker.getPasswordHash())).thenReturn(false);

            assertThrows(
                    InvalidWorkerCredentialsException.class,
                    () -> service.login(command).await().indefinitely());
        }
    }

    private CreateWorkerCommand createCommand() {
        return new CreateWorkerCommand(
                "Ana",
                "Silva",
                "ana@example.com",
                "5511999999999",
                "password123",
                WorkerProfile.MECHANIC);
    }

    private UpdateWorkerCommand updateCommand() {
        return new UpdateWorkerCommand(
                WORKER_ID,
                "Maria",
                "Souza",
                "maria@example.com",
                "5511888888888",
                WorkerProfile.MECHANIC);
    }
}
