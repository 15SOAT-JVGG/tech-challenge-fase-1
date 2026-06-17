package br.com.fiap.postech.soat16.fase1.model;

import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import br.com.fiap.postech.soat16.fase1.model.audit.AuditableEntity;

import lombok.*;

@Entity
@Table(name="vehicle", schema = "oficina_mecanica")
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

    @Column(nullable = false, length = 10)
    private VehicleType type;
}
