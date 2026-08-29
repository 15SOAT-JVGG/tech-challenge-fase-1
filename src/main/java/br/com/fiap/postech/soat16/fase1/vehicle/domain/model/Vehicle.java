package br.com.fiap.postech.soat16.fase1.vehicle.domain.model;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.customer.domain.model.Customer;
import br.com.fiap.postech.soat16.fase1.shared.domain.model.audit.AuditableEntity;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.enums.VehicleType;

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
public class Vehicle extends AuditableEntity {

    @EqualsAndHashCode.Include
    private UUID id;

    private Customer customer;

    private String licensePlate;

    private String manufacturer;

    private String model;

    private String color;

    private Integer year;

    private Long kmDriven;

    private VehicleType type;

    public static Vehicle create(Customer customer, String licensePlate, String manufacturer,
            String model, String color, Integer year, Long kmDriven, VehicleType type) {
        var vehicle = new Vehicle();
        vehicle.customer = customer;
        vehicle.update(licensePlate, manufacturer, model, color, year, kmDriven, type);
        return vehicle;
    }

    public void update(String licensePlate, String manufacturer, String model,
            String color, Integer year, Long kmDriven, VehicleType type) {
        this.licensePlate = licensePlate;
        this.manufacturer = manufacturer;
        this.model = model;
        this.color = color;
        this.year = year;
        this.kmDriven = kmDriven;
        this.type = type;
    }
}
