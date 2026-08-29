package br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.shared.application.result.PagedResult;
import br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.dto.request.WorkerLoginRequestDto;
import br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.dto.request.WorkerRequestDto;
import br.com.fiap.postech.soat16.fase1.worker.application.result.WorkerLoginResult;
import br.com.fiap.postech.soat16.fase1.worker.application.result.WorkerResult;
import br.com.fiap.postech.soat16.fase1.worker.domain.model.enums.WorkerProfile;

@DisplayName("WorkerRestMapper — Unit Tests")
class WorkerRestMapperTest {

    @Test
    @DisplayName("maps REST requests to application commands")
    void mapsCommands() {
        UUID id = UUID.randomUUID();
        WorkerRequestDto request = new WorkerRequestDto(
                "Ana",
                "Silva",
                "ana@example.com",
                "5511999999999",
                "password123",
                WorkerProfile.MECHANIC);
        WorkerLoginRequestDto login =
                new WorkerLoginRequestDto("ana@example.com", "password123");

        var create = WorkerRestMapper.toCreateCommand(request);
        var update = WorkerRestMapper.toUpdateCommand(id, request);
        var authenticate = WorkerRestMapper.toLoginCommand(login);

        assertEquals(request.password(), create.password());
        assertEquals(id, update.id());
        assertEquals(request.profile(), update.profile());
        assertEquals(login.email(), authenticate.email());
    }

    @Test
    @DisplayName("maps application results to the preserved REST contracts")
    void mapsResponses() {
        UUID id = UUID.randomUUID();
        WorkerResult worker = new WorkerResult(
                id,
                "Ana",
                "Silva",
                "ana@example.com",
                "5511999999999",
                true,
                null);
        WorkerLoginResult login =
                new WorkerLoginResult(id, "Ana", "Silva", "ana@example.com", true);

        var response = WorkerRestMapper.toResponse(worker);
        var loginResponse = WorkerRestMapper.toResponse(login);
        var page = WorkerRestMapper.toResponse(PagedResult.of(List.of(worker), 0, 10, 1));

        assertEquals(id, response.workerId());
        assertEquals(true, response.active());
        assertEquals(id, loginResponse.workerId());
        assertEquals(true, loginResponse.authenticated());
        assertEquals(1L, page.pagination().totalElements());
        assertNull(WorkerRestMapper.toResponse((WorkerResult) null));
    }
}
