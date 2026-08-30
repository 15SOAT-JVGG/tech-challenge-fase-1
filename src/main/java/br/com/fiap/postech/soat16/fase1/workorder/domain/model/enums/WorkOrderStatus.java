package br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums;

import java.util.List;

public enum WorkOrderStatus {

    RECEIVED,
    DIAGNOSIS,
    WAITING_APPROVAL,
    IN_PROGRESS,
    COMPLETED,
    DELIVERED;

    private static final List<WorkOrderStatus> OPERATIONAL_QUEUE =
            List.of(IN_PROGRESS, WAITING_APPROVAL, DIAGNOSIS, RECEIVED);

    /**
     * Os status ainda em atendimento, do estágio mais avançado para o recém-recebido — a ordem em
     * que a oficina trabalha a fila.
     */
    public static List<WorkOrderStatus> operationalQueue() {
        return OPERATIONAL_QUEUE;
    }
}
