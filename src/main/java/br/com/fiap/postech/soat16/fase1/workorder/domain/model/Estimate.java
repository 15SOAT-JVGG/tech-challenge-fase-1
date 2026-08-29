package br.com.fiap.postech.soat16.fase1.workorder.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import br.com.fiap.postech.soat16.fase1.model.audit.AuditableEntity;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.EstimateAlreadyDecidedException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.EstimateStatus;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "estimate", schema = "oficina_mecanica")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Estimate extends AuditableEntity {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "estimate_id", nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstimateStatus status;

    @Column(name = "parts_amount", precision = 10, scale = 2)
    private BigDecimal partsAmount;

    @Column(name = "labor_amount", precision = 10, scale = 2)
    private BigDecimal laborAmount;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @OneToMany(mappedBy = "estimate", cascade = CascadeType.ALL, orphanRemoval = true)
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
