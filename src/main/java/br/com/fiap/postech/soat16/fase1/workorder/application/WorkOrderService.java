package br.com.fiap.postech.soat16.fase1.workorder.application;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.customer.domain.exception.CustomerNotFoundException;
import br.com.fiap.postech.soat16.fase1.part.domain.model.Part;
import br.com.fiap.postech.soat16.fase1.servicecatalog.domain.exception.ServiceItemNotFoundException;
import br.com.fiap.postech.soat16.fase1.servicecatalog.domain.model.ServiceItem;
import br.com.fiap.postech.soat16.fase1.shared.application.result.PagedResult;
import br.com.fiap.postech.soat16.fase1.vehicle.domain.exception.VehicleNotFoundException;
import br.com.fiap.postech.soat16.fase1.worker.domain.exception.WorkerNotFoundException;
import br.com.fiap.postech.soat16.fase1.worker.domain.model.Worker;
import br.com.fiap.postech.soat16.fase1.workorder.application.command.AddWorkOrderServiceCommand;
import br.com.fiap.postech.soat16.fase1.workorder.application.command.ChangeWorkOrderStatusCommand;
import br.com.fiap.postech.soat16.fase1.workorder.application.command.CloseWorkOrderCommand;
import br.com.fiap.postech.soat16.fase1.workorder.application.command.CreateEstimateCommand;
import br.com.fiap.postech.soat16.fase1.workorder.application.command.OpenWorkOrderCommand;
import br.com.fiap.postech.soat16.fase1.workorder.application.mapper.EstimateMapper;
import br.com.fiap.postech.soat16.fase1.workorder.application.mapper.WorkOrderMapper;
import br.com.fiap.postech.soat16.fase1.workorder.application.mapper.WorkOrderServiceMapper;
import br.com.fiap.postech.soat16.fase1.workorder.application.port.out.EstimatePersistencePort;
import br.com.fiap.postech.soat16.fase1.workorder.application.port.out.WorkOrderNotificationPort;
import br.com.fiap.postech.soat16.fase1.workorder.application.port.out.WorkOrderPersistencePort;
import br.com.fiap.postech.soat16.fase1.workorder.application.port.out.WorkOrderServicePersistencePort;
import br.com.fiap.postech.soat16.fase1.workorder.application.port.out.WorkshopCatalogPort;
import br.com.fiap.postech.soat16.fase1.workorder.application.result.EstimateResult;
import br.com.fiap.postech.soat16.fase1.workorder.application.result.WorkOrderMetricsResult;
import br.com.fiap.postech.soat16.fase1.workorder.application.result.WorkOrderResult;
import br.com.fiap.postech.soat16.fase1.workorder.application.result.WorkOrderServiceResult;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.EstimateNotFoundException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.EstimatePartNotFoundException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.WorkOrderNotFoundException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.Estimate;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.EstimateItem;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrder;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrderHistory;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderStatus;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderPersistencePort repository;
    private final EstimatePersistencePort estimateRepository;
    private final WorkOrderServicePersistencePort serviceRepository;
    private final WorkshopCatalogPort catalog;
    private final WorkOrderMapper mapper;
    private final EstimateMapper estimateMapper;
    private final WorkOrderServiceMapper serviceMapper;
    private final WorkOrderNotificationPort notificationService;

    @WithSession
    public Uni<PagedResult<WorkOrderResult>> findAll(String q, int page, int size) {
        return Uni.combine().all().unis(repository.findPage(page, size), repository.countWorkOrders()).asTuple()
                .map(tuple -> PagedResult.of(
                        tuple.getItem1().stream().map(mapper::toResult).toList(),
                        page,
                        size,
                        tuple.getItem2()));
    }

    @WithSession
    public Uni<WorkOrderMetricsResult> averageExecutionTime() {
        return repository.findClosed().map(orders -> {
            if (orders.isEmpty()) {
                return new WorkOrderMetricsResult(0, null);
            }
            double averageMinutes = orders.stream()
                    .mapToDouble(o -> Duration.between(o.getOpenedAt(), o.getClosedAt()).toSeconds() / 60.0)
                    .average()
                    .orElse(0);
            return new WorkOrderMetricsResult(orders.size(), Math.round(averageMinutes * 100) / 100.0);
        });
    }

    @WithSession
    public Uni<WorkOrderResult> findById(UUID id) {
        return repository.findByWorkOrderId(id)
                .onItem().ifNull().failWith(WorkOrderNotFoundException::new)
                .map(mapper::toResult);
    }

    @WithTransaction
    public Uni<Void> create(OpenWorkOrderCommand request) {
        return catalog.findCustomerById(request.customerId())
                .onItem().ifNull().failWith(CustomerNotFoundException::new)
                .flatMap(customer -> catalog.findVehicleById(request.vehicleId())
                        .onItem().ifNull().failWith(VehicleNotFoundException::new)
                        .flatMap(vehicle -> resolveAssignedWorker(request.assignedWorkerId())
                                .flatMap(worker -> repository.save(WorkOrder.open(
                                        customer,
                                        vehicle,
                                        worker,
                                        request.description(),
                                        request.priority(),
                                        LocalDateTime.now(ZoneId.systemDefault())))
                                        .replaceWithVoid())));
    }

    private Uni<Worker> resolveAssignedWorker(UUID assignedWorkerId) {
        if (assignedWorkerId == null) {
            return Uni.createFrom().nullItem();
        }
        return catalog.findWorkerById(assignedWorkerId)
                .onItem().ifNull().failWith(() -> new WorkerNotFoundException(assignedWorkerId));
    }

    @WithTransaction
    public Uni<WorkOrderResult> updateStatus(UUID id, ChangeWorkOrderStatusCommand request) {
        return repository.findByWorkOrderId(id)
                .onItem().ifNull().failWith(WorkOrderNotFoundException::new)
                .flatMap(order -> {
                    order.ensureMutable();
                    Uni<Boolean> approvedEstimate = requiresApprovedEstimate(request.status())
                            ? hasApprovedEstimate(id)
                            : Uni.createFrom().item(false);
                    return approvedEstimate
                            .flatMap(hasApproved -> {
                                var history = order.transitionTo(
                                        request.status(),
                                        hasApproved,
                                        LocalDateTime.now(ZoneId.systemDefault()));
                                return persistOrderWithHistory(order, Optional.of(history));
                            })
                            .map(mapper::toResult);
                });
    }

    @WithTransaction
    public Uni<EstimateResult> createEstimate(UUID workOrderId, CreateEstimateCommand request) {
        return repository.findByWorkOrderId(workOrderId)
                .onItem().ifNull().failWith(WorkOrderNotFoundException::new)
                .flatMap(order -> {
                    order.ensureMutable();
                    return buildItems(request)
                            .flatMap(items -> serviceRepository.findByWorkOrderId(workOrderId)
                                    .flatMap(services -> persistEstimate(order, items, services)))
                            .flatMap(estimate -> finalizeEstimateCreation(order, estimate));
                })
                .map(estimateMapper::toResult);
    }

    @WithTransaction
    public Uni<EstimateResult> approveEstimate(UUID workOrderId, UUID estimateId) {
        return repository.findByWorkOrderId(workOrderId)
                .onItem().ifNull().failWith(WorkOrderNotFoundException::new)
                .flatMap(order -> {
                    order.ensureMutable();
                    return estimateRepository.findByEstimateIdAndWorkOrderId(estimateId, workOrderId)
                            .onItem().ifNull().failWith(EstimateNotFoundException::new)
                            .flatMap(estimate -> approve(order, estimate));
                })
                .map(estimateMapper::toResult);
    }

    @WithTransaction
    public Uni<EstimateResult> rejectEstimate(UUID workOrderId, UUID estimateId) {
        return repository.findByWorkOrderId(workOrderId)
                .onItem().ifNull().failWith(WorkOrderNotFoundException::new)
                .flatMap(order -> {
                    order.ensureMutable();
                    return estimateRepository.findByEstimateIdAndWorkOrderId(estimateId, workOrderId)
                            .onItem().ifNull().failWith(EstimateNotFoundException::new)
                            .flatMap(estimate -> reject(order, estimate));
                })
                .map(estimateMapper::toResult);
    }

    @WithTransaction
    public Uni<WorkOrderServiceResult> addService(UUID workOrderId, AddWorkOrderServiceCommand request) {
        return repository.findByWorkOrderId(workOrderId)
                .onItem().ifNull().failWith(WorkOrderNotFoundException::new)
                .flatMap(order -> {
                    order.ensureMutable();
                    return resolveServiceItem(request.serviceItemId())
                            .flatMap(serviceItem -> serviceRepository.save(
                                    serviceMapper.toEntity(request, order, serviceItem)));
                })
                .map(serviceMapper::toResult);
    }

    private Uni<ServiceItem> resolveServiceItem(UUID serviceItemId) {
        if (serviceItemId == null) {
            return Uni.createFrom().nullItem();
        }
        return catalog.findServiceItemById(serviceItemId)
                .onItem().ifNull().failWith(() -> new ServiceItemNotFoundException(serviceItemId));
    }

    @WithTransaction
    public Uni<WorkOrderResult> close(UUID id, CloseWorkOrderCommand request) {
        return repository.findByWorkOrderId(id)
                .onItem().ifNull().failWith(WorkOrderNotFoundException::new)
                .flatMap(order -> {
                    order.ensureClosable();
                    return hasApprovedEstimate(id)
                            .flatMap(hasApproved -> persistOrderWithHistory(
                                    order,
                                    Optional.of(order.close(
                                            request.finalValue(),
                                            hasApproved,
                                            LocalDateTime.now(ZoneId.systemDefault())))))
                            .flatMap(saved -> notificationService.notifyWorkOrderCompleted(saved).replaceWith(saved))
                            .map(mapper::toResult);
                });
    }

    private Uni<Estimate> approve(WorkOrder order, Estimate estimate) {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        estimate.assertPending();
        return reserveStock(estimate)
                .flatMap(ignored -> {
                    estimate.approve(now);
                    return persistOrderWithHistory(order, order.registerEstimateApproval(estimate, now))
                            .flatMap(persisted -> estimateRepository.save(estimate));
                });
    }

    private Uni<Estimate> reject(WorkOrder order, Estimate estimate) {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        estimate.reject();
        return persistOrderWithHistory(order, order.registerEstimateRejection(now))
                .flatMap(persisted -> estimateRepository.save(estimate));
    }

    private Uni<Estimate> finalizeEstimateCreation(WorkOrder order, Estimate estimate) {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        estimate.markSent(now);
        return persistOrderWithHistory(order, order.registerEstimate(estimate, now))
                .flatMap(saved -> notificationService.notifyEstimateReady(saved, estimate))
                .replaceWith(estimate);
    }

    private Uni<List<EstimateItem>> buildItems(CreateEstimateCommand request) {
        List<Uni<EstimateItem>> itemUnis = request.items().stream()
                .map(itemDto -> catalog.findPartById(itemDto.partId())
                        .onItem().ifNull().failWith(() -> new EstimatePartNotFoundException(itemDto.partId()))
                        .map(part -> EstimateItem.create(part, itemDto.quantity(), itemDto.unitPrice())))
                .toList();
        return Uni.join().all(itemUnis).andFailFast();
    }

    private Uni<Estimate> persistEstimate(WorkOrder order, List<EstimateItem> items,
            List<br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrderService> services) {
        return estimateRepository.save(Estimate.create(order, items, services));
    }

    /**
     * Baixa o estoque somente após a aprovação do orçamento. A transação é revertida se alguma peça
     * não tiver estoque suficiente.
     */
    private Uni<Void> reserveStock(Estimate estimate) {
        List<Part> parts = estimate.getItems().stream()
                .map(item -> {
                    Part part = item.getPart();
                    part.decreaseStock(item.getQuantity());
                    if (part.isLowStock()) {
                        Log.warnf("Estoque baixo apos reserva: peca=%s restante=%d minimo=%d",
                                part.getName(), part.getStockQuantity(), part.getMinimumStock());
                    }
                    return part;
                })
                .toList();
        return catalog.saveParts(parts);
    }

    private Uni<WorkOrder> persistOrderWithHistory(WorkOrder order,
            Optional<WorkOrderHistory> history) {
        return history
                .map(value -> repository.saveWithHistory(order, value))
                .orElseGet(() -> repository.save(order));
    }

    private Uni<Boolean> hasApprovedEstimate(UUID workOrderId) {
        return estimateRepository.existsApprovedByWorkOrderId(workOrderId)
                .map(Boolean.TRUE::equals);
    }

    private boolean requiresApprovedEstimate(WorkOrderStatus target) {
        return target == WorkOrderStatus.IN_PROGRESS;
    }
}
