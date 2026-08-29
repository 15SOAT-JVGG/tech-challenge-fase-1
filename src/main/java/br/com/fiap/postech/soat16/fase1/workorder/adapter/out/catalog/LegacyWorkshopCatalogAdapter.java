package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.catalog;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.customer.application.port.out.CustomerPersistencePort;
import br.com.fiap.postech.soat16.fase1.customer.domain.model.Customer;
import br.com.fiap.postech.soat16.fase1.part.application.port.out.PartPersistencePort;
import br.com.fiap.postech.soat16.fase1.part.domain.model.Part;
import br.com.fiap.postech.soat16.fase1.servicecatalog.application.port.out.ServiceCatalogPersistencePort;
import br.com.fiap.postech.soat16.fase1.servicecatalog.domain.model.ServiceItem;
import br.com.fiap.postech.soat16.fase1.vehicle.application.port.out.VehiclePersistencePort;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.model.Vehicle;
import br.com.fiap.postech.soat16.fase1.worker.application.port.out.WorkerPersistencePort;
import br.com.fiap.postech.soat16.fase1.worker.domain.model.Worker;
import br.com.fiap.postech.soat16.fase1.workorder.application.port.out.WorkshopCatalogPort;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class LegacyWorkshopCatalogAdapter implements WorkshopCatalogPort {

    private final CustomerPersistencePort customerRepository;
    private final VehiclePersistencePort vehicleRepository;
    private final PartPersistencePort partRepository;
    private final WorkerPersistencePort workerRepository;
    private final ServiceCatalogPersistencePort serviceItemRepository;

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
        return partRepository.findPartById(id);
    }

    @Override
    public Uni<Worker> findWorkerById(UUID id) {
        return workerRepository.findByWorkerId(id);
    }

    @Override
    public Uni<ServiceItem> findServiceItemById(UUID id) {
        return serviceItemRepository.findServiceItemById(id);
    }
}
