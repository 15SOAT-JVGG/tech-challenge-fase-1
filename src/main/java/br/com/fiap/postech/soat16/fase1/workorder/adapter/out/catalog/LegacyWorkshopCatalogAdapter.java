package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.catalog;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.model.Customer;
import br.com.fiap.postech.soat16.fase1.model.Part;
import br.com.fiap.postech.soat16.fase1.model.ServiceItem;
import br.com.fiap.postech.soat16.fase1.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.model.Worker;
import br.com.fiap.postech.soat16.fase1.repository.CustomerRepository;
import br.com.fiap.postech.soat16.fase1.repository.PartRepository;
import br.com.fiap.postech.soat16.fase1.repository.ServiceItemRepository;
import br.com.fiap.postech.soat16.fase1.repository.VehicleRepository;
import br.com.fiap.postech.soat16.fase1.repository.WorkerRepository;
import br.com.fiap.postech.soat16.fase1.workorder.application.port.out.WorkshopCatalogPort;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class LegacyWorkshopCatalogAdapter implements WorkshopCatalogPort {

    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final PartRepository partRepository;
    private final WorkerRepository workerRepository;
    private final ServiceItemRepository serviceItemRepository;

    @Override
    public Uni<Customer> findCustomerById(UUID id) {
        return customerRepository.findByCustomerId(id);
    }

    @Override
    public Uni<Vehicle> findVehicleById(UUID id) {
        return vehicleRepository.findByVehicleId(id);
    }

    @Override
    public Uni<Part> findPartById(UUID id) {
        return partRepository.findById(id);
    }

    @Override
    public Uni<Worker> findWorkerById(UUID id) {
        return workerRepository.findByWorkerId(id);
    }

    @Override
    public Uni<ServiceItem> findServiceItemById(UUID id) {
        return serviceItemRepository.findById(id);
    }
}
