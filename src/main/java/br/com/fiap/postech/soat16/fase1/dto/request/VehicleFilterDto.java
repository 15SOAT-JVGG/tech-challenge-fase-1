package br.com.fiap.postech.soat16.fase1.dto.request;

import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

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
        return licensePlate == null ? null : licensePlate.replace("-", "").toUpperCase();
    }
}