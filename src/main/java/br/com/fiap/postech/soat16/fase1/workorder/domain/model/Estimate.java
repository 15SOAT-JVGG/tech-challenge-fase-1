package br.com.fiap.postech.soat16.fase1.workorder.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.shared.domain.model.audit.AuditableEntity;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.EstimateAlreadyDecidedException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.EstimateStatus;

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
public class Estimate extends AuditableEntity {

    @EqualsAndHashCode.Include
    private UUID id;

    private WorkOrder workOrder;

    private EstimateStatus status;

    private BigDecimal partsAmount;

    private BigDecimal laborAmount;

    private BigDecimal totalAmount;

    private LocalDateTime approvedAt;

    private LocalDateTime sentAt;

    private List<EstimateItem> items = new ArrayList<>();

    public static Estimate create(WorkOrder workOrder, List<EstimateItem> items,
            List<WorkOrderService> laborServices) {
        var estimate = new Estimate();
        var partsAmount = items.stream()
                .map(EstimateItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var laborAmount = laborServices.stream()
                .map(WorkOrderService::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        estimate.workOrder = workOrder;
        estimate.status = EstimateStatus.PENDING;
        estimate.partsAmount = partsAmount;
        estimate.laborAmount = laborAmount;
        estimate.totalAmount = partsAmount.add(laborAmount);
        estimate.items = new ArrayList<>(items);
        estimate.items.forEach(item -> item.setEstimate(estimate));
        return estimate;
    }

    public void approve(LocalDateTime approvedAt) {
        assertPending();
        status = EstimateStatus.APPROVED;
        this.approvedAt = approvedAt;
    }

    public void reject() {
        assertPending();
        status = EstimateStatus.REJECTED;
    }

    public void markSent(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public void assertPending() {
        if (status != EstimateStatus.PENDING) {
            throw new EstimateAlreadyDecidedException();
        }
    }
}
