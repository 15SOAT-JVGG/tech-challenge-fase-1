package br.com.fiap.postech.soat16.fase1.vehicle.adapter.in.rest.dto.request;

import java.util.Locale;

import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import lombok.Getter;

@Getter
public class VehicleFilterDto {

    @Parameter(description = "Filter by license plate (partial, case-insensitive)")
    @QueryParam("license_plate")
    private String licensePlate;

    @Parameter(description = "Filter by manufacturer (partial, case-insensitive)")
    @QueryParam("manufacturer")
    private String manufacturer;

    @Parameter(description = "Filter by model (partial, case-insensitive)")
    @QueryParam("model")
    private String model;

    public String getLicensePlate() {
        return licensePlate == null
                ? null
                : licensePlate.replace("-", "").toUpperCase(Locale.ROOT);
    }
}
