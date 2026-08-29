package br.com.fiap.postech.soat16.fase1.workorder.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderStatus;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class WorkOrderHistory {

    @EqualsAndHashCode.Include
    private UUID id;

    private WorkOrder workOrder;

    private WorkOrderStatus previousStatus;

    private WorkOrderStatus newStatus;

    private LocalDateTime changedAt;
}
