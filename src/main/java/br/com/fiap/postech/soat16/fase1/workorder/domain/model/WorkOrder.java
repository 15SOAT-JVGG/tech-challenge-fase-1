package br.com.fiap.postech.soat16.fase1.workorder.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

import br.com.fiap.postech.soat16.fase1.model.Customer;
import br.com.fiap.postech.soat16.fase1.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.model.Worker;
import br.com.fiap.postech.soat16.fase1.model.audit.AuditableEntity;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.EstimateNotApprovedException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.InvalidWorkOrderStatusTransitionException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.WorkOrderLockedException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderPriority;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderStatus;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "work_order", schema = "oficina_mecanica")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class WorkOrder extends AuditableEntity {

    private static final Map<WorkOrderStatus, WorkOrderStatus> FORWARD_TRANSITIONS = Map.of(
            WorkOrderStatus.RECEIVED, WorkOrderStatus.DIAGNOSIS,
            WorkOrderStatus.DIAGNOSIS, WorkOrderStatus.WAITING_APPROVAL,
            WorkOrderStatus.WAITING_APPROVAL, WorkOrderStatus.APPROVED,
            WorkOrderStatus.APPROVED, WorkOrderStatus.IN_PROGRESS
    );

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "work_order_id", nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "assigned_worker_id")
    private Worker assignedWorker;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private WorkOrderPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkOrderStatus status;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "estimated_value", precision = 10, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "final_value", precision = 10, scale = 2)
    private BigDecimal finalValue;

    public static WorkOrder open(Customer customer, Vehicle vehicle, Worker assignedWorker,
            String description, WorkOrderPriority priority, LocalDateTime openedAt) {
        var workOrder = new WorkOrder();
        workOrder.customer = customer;
        workOrder.vehicle = vehicle;
        workOrder.assignedWorker = assignedWorker;
        workOrder.description = description;
        workOrder.priority = priority != null ? priority : WorkOrderPriority.MEDIUM;
        workOrder.status = WorkOrderStatus.RECEIVED;
        workOrder.openedAt = openedAt;
        return workOrder;
    }

    public WorkOrderHistory transitionTo(WorkOrderStatus target, boolean hasApprovedEstimate,
            LocalDateTime changedAt) {
        ensureMutable();
        validateGenericTransition(target);
        ensureApprovedEstimateWhenRequired(target, hasApprovedEstimate);
        return applyStatus(target, changedAt);
    }

    public WorkOrderHistory close(BigDecimal requestedFinalValue, boolean hasApprovedEstimate,
            LocalDateTime changedAt) {
        ensureClosable();
        ensureApprovedEstimateWhenRequired(WorkOrderStatus.IN_PROGRESS, hasApprovedEstimate);
        finalValue = requestedFinalValue != null ? requestedFinalValue : estimatedValue;
        closedAt = changedAt;
        return applyStatus(WorkOrderStatus.COMPLETED, changedAt);
    }

    public Optional<WorkOrderHistory> registerEstimate(Estimate estimate, LocalDateTime changedAt) {
        ensureMutable();
        estimatedValue = estimate.getTotalAmount();
        if (status == WorkOrderStatus.DIAGNOSIS) {
            return Optional.of(applyStatus(WorkOrderStatus.WAITING_APPROVAL, changedAt));
        }
        return Optional.empty();
    }

    public Optional<WorkOrderHistory> registerEstimateApproval(Estimate estimate,
            LocalDateTime changedAt) {
        ensureMutable();
        estimatedValue = estimate.getTotalAmount();
        if (status == WorkOrderStatus.WAITING_APPROVAL) {
            return Optional.of(applyStatus(WorkOrderStatus.APPROVED, changedAt));
        }
        return Optional.empty();
    }

    public Optional<WorkOrderHistory> registerEstimateRejection(LocalDateTime changedAt) {
        ensureMutable();
        if (status == WorkOrderStatus.WAITING_APPROVAL) {
            return Optional.of(applyStatus(WorkOrderStatus.DIAGNOSIS, changedAt));
        }
        return Optional.empty();
    }

    public boolean hasReservedStock() {
        return status == WorkOrderStatus.APPROVED;
    }

    public void ensureMutable() {
        if (status == WorkOrderStatus.DELIVERED || status == WorkOrderStatus.CANCELLED) {
            throw new WorkOrderLockedException();
        }
    }

    public void ensureClosable() {
        ensureMutable();
        if (status != WorkOrderStatus.IN_PROGRESS) {
            throw new InvalidWorkOrderStatusTransitionException(
                    "Only work orders IN_PROGRESS can be closed");
        }
    }

    private void validateGenericTransition(WorkOrderStatus target) {
        if (target == WorkOrderStatus.COMPLETED) {
            throw new InvalidWorkOrderStatusTransitionException(
                    "Use PATCH /v1/work-orders/{id}/close to complete a work order");
        }
        if (target == WorkOrderStatus.CANCELLED) {
            return;
        }
        if (target == WorkOrderStatus.DELIVERED) {
            if (status != WorkOrderStatus.COMPLETED) {
                throw new InvalidWorkOrderStatusTransitionException(status, target);
            }
            return;
        }
        if (target != FORWARD_TRANSITIONS.get(status)) {
            throw new InvalidWorkOrderStatusTransitionException(status, target);
        }
    }

    private void ensureApprovedEstimateWhenRequired(WorkOrderStatus target,
            boolean hasApprovedEstimate) {
        if ((target == WorkOrderStatus.APPROVED || target == WorkOrderStatus.IN_PROGRESS)
                && !hasApprovedEstimate) {
            throw new EstimateNotApprovedException();
        }
    }

    private WorkOrderHistory applyStatus(WorkOrderStatus target, LocalDateTime changedAt) {
        var history = new WorkOrderHistory();
        history.setWorkOrder(this);
        history.setPreviousStatus(status);
        history.setNewStatus(target);
        history.setChangedAt(changedAt);
        status = target;
        return history;
    }
}
