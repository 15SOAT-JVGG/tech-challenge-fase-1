package br.com.fiap.postech.soat16.fase1.servicecatalog.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import br.com.fiap.postech.soat16.fase1.shared.adapter.out.persistence.AuditableJpaEntity;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "ServiceItem")
@Table(name = "service_item", schema = "oficina_mecanica")
@Getter
@Setter
public class ServiceItemJpaEntity extends AuditableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "service_item_id", nullable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(nullable = false)
    private boolean active;
}
