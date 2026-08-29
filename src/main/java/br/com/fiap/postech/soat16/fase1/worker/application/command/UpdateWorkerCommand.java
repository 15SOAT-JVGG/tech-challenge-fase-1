package br.com.fiap.postech.soat16.fase1.worker.application.command;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.worker.domain.model.enums.WorkerProfile;

public record UpdateWorkerCommand(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        WorkerProfile profile
) { }
