package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import br.com.fiap.postech.soat16.fase1.servicecatalog.adapter.out.persistence.entity.ServiceItemJpaEntity;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "WorkOrderService")
@Table(name = "work_order_service", schema = "oficina_mecanica")
@Getter
@Setter
public class WorkOrderServiceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "work_order_service_id", nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrderJpaEntity workOrder;

    @ManyToOne
    @JoinColumn(name = "service_item_id")
    private ServiceItemJpaEntity serviceItem;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;
}
