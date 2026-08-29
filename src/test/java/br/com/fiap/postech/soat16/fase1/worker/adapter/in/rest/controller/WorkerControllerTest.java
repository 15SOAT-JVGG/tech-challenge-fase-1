package br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.shared.application.result.PagedResult;
import br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.dto.request.WorkerLoginRequestDto;
import br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.dto.request.WorkerRequestDto;
import br.com.fiap.postech.soat16.fase1.worker.application.WorkerService;
import br.com.fiap.postech.soat16.fase1.worker.application.command.CreateWorkerCommand;
import br.com.fiap.postech.soat16.fase1.worker.application.command.LoginWorkerCommand;
import br.com.fiap.postech.soat16.fase1.worker.application.command.UpdateWorkerCommand;
import br.com.fiap.postech.soat16.fase1.worker.application.result.WorkerLoginResult;
import br.com.fiap.postech.soat16.fase1.worker.application.result.WorkerResult;
import br.com.fiap.postech.soat16.fase1.worker.domain.model.enums.WorkerProfile;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkerController — Unit Tests")
class WorkerControllerTest {

    private static final UUID WORKER_ID =
            UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @Mock
    WorkerService service;

    private WorkerController controller;
    private WorkerResult workerResult;

    @BeforeEach
    void setUp() {
        controller = new WorkerController(service);
        workerResult = new WorkerResult(
                WORKER_ID,
                "Ana",
                "Silva",
                "ana@example.com",
                "5511999999999",
                true,
                null);
    }

    @Test
    @DisplayName("maps application pagination to the preserved REST contract")
    void listsWorkers() {
        PageableRequestDto pageable = mock(PageableRequestDto.class);
        when(pageable.getQ()).thenReturn("ignored");
        when(pageable.getPage()).thenReturn(0);
        when(pageable.getSize()).thenReturn(10);
        when(service.findAll("ignored", 0, 10))
                .thenReturn(Uni.createFrom().item(PagedResult.of(List.of(workerResult), 0, 10, 1)));

        var response = controller.findAll(pageable).await().indefinitely();

        assertEquals(1, response.content().size());
        assertEquals(1L, response.pagination().totalElements());
    }

    @Test
    @DisplayName("returns a worker by id")
    void findsWorker() {
        when(service.findById(WORKER_ID)).thenReturn(Uni.createFrom().item(workerResult));

        var response = controller.findById(WORKER_ID).await().indefinitely();

        assertEquals(WORKER_ID, response.workerId());
        verify(service).findById(WORKER_ID);
    }

    @Test
    @DisplayName("returns HTTP 201 when creation succeeds")
    void createsWorker() {
        WorkerRequestDto request = workerRequest();
        CreateWorkerCommand command = new CreateWorkerCommand(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phoneNumber(),
                request.password(),
                request.profile());
        when(service.create(command)).thenReturn(Uni.createFrom().voidItem());

        Response response = controller.create(request).await().indefinitely();

        assertEquals(201, response.getStatus());
        verify(service).create(command);
    }

    @Test
    @DisplayName("keeps worker login separate and returns its response")
    void logsWorkerIn() {
        WorkerLoginRequestDto request =
                new WorkerLoginRequestDto("ana@example.com", "password123");
        LoginWorkerCommand command =
                new LoginWorkerCommand(request.email(), request.password());
        WorkerLoginResult login = new WorkerLoginResult(
                WORKER_ID, "Ana", "Silva", "ana@example.com", true);
        when(service.login(command)).thenReturn(Uni.createFrom().item(login));

        var response = controller.login(request).await().indefinitely();

        assertNotNull(response);
        assertEquals(WORKER_ID, response.workerId());
        assertEquals(true, response.authenticated());
    }

    @Test
    @DisplayName("returns HTTP 200 and the preserved body when update succeeds")
    void updatesWorker() {
        WorkerRequestDto request = workerRequest();
        UpdateWorkerCommand command = new UpdateWorkerCommand(
                WORKER_ID,
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phoneNumber(),
                request.profile());
        when(service.update(command)).thenReturn(Uni.createFrom().item(workerResult));

        Response response = controller.update(WORKER_ID, request).await().indefinitely();

        assertEquals(200, response.getStatus());
        verify(service).update(command);
    }

    @Test
    @DisplayName("returns HTTP 204 when deletion succeeds")
    void deletesWorker() {
        when(service.delete(WORKER_ID)).thenReturn(Uni.createFrom().voidItem());

        Response response = controller.delete(WORKER_ID).await().indefinitely();

        assertEquals(204, response.getStatus());
        verify(service).delete(WORKER_ID);
    }

    private WorkerRequestDto workerRequest() {
        return new WorkerRequestDto(
                "Ana",
                "Silva",
                "ana@example.com",
                "5511999999999",
                "password123",
                WorkerProfile.MECHANIC);
    }
}
