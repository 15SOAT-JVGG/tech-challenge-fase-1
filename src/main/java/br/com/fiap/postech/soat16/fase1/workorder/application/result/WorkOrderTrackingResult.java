package br.com.fiap.postech.soat16.fase1.workorder.application.result;

import java.time.LocalDateTime;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderStatus;

/**
 * O que o cliente vê pelo link de acompanhamento: apenas o andamento do atendimento. Valores,
 * peças, mecânico e descrição ficam fora — o link viaja por e-mail e pode ser reencaminhado.
 */
public record WorkOrderTrackingResult(
        UUID workOrderId,
        WorkOrderStatus status,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        LocalDateTime cancelledAt
) { }
