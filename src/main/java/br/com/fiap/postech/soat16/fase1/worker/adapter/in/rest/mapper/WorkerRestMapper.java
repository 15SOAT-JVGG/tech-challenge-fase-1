package br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.mapper;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.shared.adapter.in.rest.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.shared.application.result.PagedResult;
import br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.dto.request.WorkerLoginRequestDto;
import br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.dto.request.WorkerRequestDto;
import br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.dto.response.WorkerLoginResponseDto;
import br.com.fiap.postech.soat16.fase1.worker.adapter.in.rest.dto.response.WorkerResponseDto;
import br.com.fiap.postech.soat16.fase1.worker.application.command.CreateWorkerCommand;
import br.com.fiap.postech.soat16.fase1.worker.application.command.LoginWorkerCommand;
import br.com.fiap.postech.soat16.fase1.worker.application.command.UpdateWorkerCommand;
import br.com.fiap.postech.soat16.fase1.worker.application.result.WorkerLoginResult;
import br.com.fiap.postech.soat16.fase1.worker.application.result.WorkerResult;

public final class WorkerRestMapper {

    private WorkerRestMapper() {
    }

    public static CreateWorkerCommand toCreateCommand(WorkerRequestDto request) {
        return new CreateWorkerCommand(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phoneNumber(),
                request.password(),
                request.profile());
    }

    public static UpdateWorkerCommand toUpdateCommand(UUID id, WorkerRequestDto request) {
        return new UpdateWorkerCommand(
                id,
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phoneNumber(),
                request.profile());
    }

    public static LoginWorkerCommand toLoginCommand(WorkerLoginRequestDto request) {
        return new LoginWorkerCommand(request.email(), request.password());
    }

    public static PageableResponseDto<WorkerResponseDto> toResponse(
            PagedResult<WorkerResult> page) {
        return PageableResponseDto.of(
                page.content().stream().map(WorkerRestMapper::toResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements());
    }

    public static WorkerResponseDto toResponse(WorkerResult result) {
        if (result == null) {
            return null;
        }
        return new WorkerResponseDto(
                result.workerId(),
                result.firstName(),
                result.lastName(),
                result.email(),
                result.phoneNumber(),
                result.active(),
                result.createdAt());
    }

    public static WorkerLoginResponseDto toResponse(WorkerLoginResult result) {
        return new WorkerLoginResponseDto(
                result.workerId(),
                result.firstName(),
                result.lastName(),
                result.email(),
                result.authenticated());
    }
}
