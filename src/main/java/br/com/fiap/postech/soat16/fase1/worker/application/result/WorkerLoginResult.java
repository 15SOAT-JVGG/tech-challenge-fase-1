package br.com.fiap.postech.soat16.fase1.worker.application.result;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.worker.domain.model.Worker;

public record WorkerLoginResult(
        UUID workerId,
        String firstName,
        String lastName,
        String email,
        boolean authenticated
) {

    public static WorkerLoginResult from(Worker worker) {
        return new WorkerLoginResult(
                worker.getId(),
                worker.getFirstName(),
                worker.getLastName(),
                worker.getEmail(),
                true);
    }
}
