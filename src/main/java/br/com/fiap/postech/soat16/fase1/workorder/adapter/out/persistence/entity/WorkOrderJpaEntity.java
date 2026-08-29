package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

import br.com.fiap.postech.soat16.fase1.customer.adapter.out.persistence.entity.CustomerJpaEntity;
import br.com.fiap.postech.soat16.fase1.shared.adapter.out.persistence.AuditableJpaEntity;
import br.com.fiap.postech.soat16.fase1.vehicle.adapter.out.persistence.entity.VehicleJpaEntity;
import br.com.fiap.postech.soat16.fase1.worker.adapter.out.persistence.entity.WorkerJpaEntity;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderPriority;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderStatus;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "WorkOrder")
@Table(name = "work_order", schema = "oficina_mecanica")
@Getter
@Setter
public class WorkOrderJpaEntity extends AuditableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "work_order_id", nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerJpaEntity customer;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private VehicleJpaEntity vehicle;

    @ManyToOne
    @JoinColumn(name = "assigned_worker_id")
    private WorkerJpaEntity assignedWorker;

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
}
