package br.com.fiap.postech.soat16.fase1.model;

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

import br.com.fiap.postech.soat16.fase1.model.audit.AuditableEntity;
import br.com.fiap.postech.soat16.fase1.model.enums.VehicleType;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehicle", schema = "oficina_mecanica")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Vehicle extends AuditableEntity {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "vehicle_id", nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

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
