package br.com.fiap.postech.soat16.fase1.workorder.application.port.out;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.model.Customer;
import br.com.fiap.postech.soat16.fase1.model.Part;
import br.com.fiap.postech.soat16.fase1.model.ServiceItem;
import br.com.fiap.postech.soat16.fase1.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.model.Worker;

import io.smallrye.mutiny.Uni;

public interface WorkshopCatalogPort {

    Uni<Customer> findCustomerById(UUID id);

    Uni<Vehicle> findVehicleById(UUID id);

    Uni<Part> findPartById(UUID id);

    Uni<Worker> findWorkerById(UUID id);

    Uni<ServiceItem> findServiceItemById(UUID id);
}
