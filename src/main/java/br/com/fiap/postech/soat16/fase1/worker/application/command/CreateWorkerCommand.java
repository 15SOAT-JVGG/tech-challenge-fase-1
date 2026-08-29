package br.com.fiap.postech.soat16.fase1.worker.application.command;

import br.com.fiap.postech.soat16.fase1.worker.domain.model.enums.WorkerProfile;

public record CreateWorkerCommand(
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String password,
        WorkerProfile profile
) { }
