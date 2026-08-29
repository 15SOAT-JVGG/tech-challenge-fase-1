package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.entity;

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

import br.com.fiap.postech.soat16.fase1.shared.adapter.out.persistence.AuditableJpaEntity;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.EstimateStatus;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "Estimate")
@Table(name = "estimate", schema = "oficina_mecanica")
@Getter
@Setter
public class EstimateJpaEntity extends AuditableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "estimate_id", nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrderJpaEntity workOrder;

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
    private List<EstimateItemJpaEntity> items = new ArrayList<>();
}
