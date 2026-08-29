package br.com.fiap.postech.soat16.fase1.vehicle.adapter.out.persistence.entity;

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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import br.com.fiap.postech.soat16.fase1.customer.adapter.out.persistence.entity.CustomerJpaEntity;
import br.com.fiap.postech.soat16.fase1.shared.adapter.out.persistence.AuditableJpaEntity;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.enums.VehicleType;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "Vehicle")
@Table(name = "vehicle", schema = "oficina_mecanica")
@Getter
@Setter
public class VehicleJpaEntity extends AuditableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "vehicle_id", nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private CustomerJpaEntity customer;

    @Pattern(regexp = "^[A-Z]{3}\\d[A-Z\\d]\\d{2}$", message = "Invalid license plate")
    @Column(name = "license_plate", nullable = false, length = 7, unique = true)
    private String licensePlate;

    @Column(nullable = false, length = 200)
    private String manufacturer;

    @Column(nullable = false, length = 200)
    private String model;

    @Column(nullable = false, length = 100)
    private String color;

    @Column(nullable = false, length = 4)
    private Integer year;

    @Min(0)
    @Column(name = "km_driven", nullable = false)
    private Long kmDriven;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VehicleType type;
}
