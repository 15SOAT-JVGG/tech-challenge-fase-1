package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderStatus;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "WorkOrderHistory")
@Table(name = "work_order_history", schema = "oficina_mecanica",
        check = @CheckConstraint(name = "ck_work_order_history_status_canonical",
                constraint = "(previous_status is null or previous_status in "
                        + WorkOrderJpaEntity.CANONICAL_STATUS_VALUES
                        + ") and new_status in "
                        + WorkOrderJpaEntity.CANONICAL_STATUS_VALUES))
@Getter
@Setter
public class WorkOrderHistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "work_order_history_id", nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrderJpaEntity workOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private WorkOrderStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private WorkOrderStatus newStatus;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}
