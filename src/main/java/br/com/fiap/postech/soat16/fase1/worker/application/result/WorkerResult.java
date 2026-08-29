package br.com.fiap.postech.soat16.fase1.worker.application.result;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.worker.domain.model.Worker;

public record WorkerResult(
        UUID workerId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        Boolean active,
        OffsetDateTime createdAt
) {

    public static WorkerResult from(Worker worker) {
        if (worker == null) {
            return null;
        }
        return new WorkerResult(
                worker.getId(),
                worker.getFirstName(),
                worker.getLastName(),
                worker.getEmail(),
                worker.getPhoneNumber(),
                worker.isActive(),
                worker.getCreatedAt());
    }
}
