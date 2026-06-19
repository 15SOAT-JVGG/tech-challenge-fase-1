package br.com.fiap.postech.soat16.fase1.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.request.EstimateRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.WorkOrderCloseRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.WorkOrderRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.WorkOrderServiceRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.WorkOrderStatusUpdateRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.EstimateResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkOrderResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkOrderServiceResponseDto;
import br.com.fiap.postech.soat16.fase1.exception.CustomerNotFoundException;
import br.com.fiap.postech.soat16.fase1.exception.EstimateAlreadyDecidedException;
import br.com.fiap.postech.soat16.fase1.exception.EstimateNotApprovedException;
import br.com.fiap.postech.soat16.fase1.exception.EstimateNotFoundException;
import br.com.fiap.postech.soat16.fase1.exception.EstimatePartNotFoundException;
import br.com.fiap.postech.soat16.fase1.exception.InvalidWorkOrderStatusTransitionException;
import br.com.fiap.postech.soat16.fase1.exception.VehicleNotFoundException;
import br.com.fiap.postech.soat16.fase1.exception.WorkOrderLockedException;
import br.com.fiap.postech.soat16.fase1.exception.WorkOrderNotFoundException;
import br.com.fiap.postech.soat16.fase1.mapper.EstimateMapper;
import br.com.fiap.postech.soat16.fase1.mapper.WorkOrderMapper;
import br.com.fiap.postech.soat16.fase1.mapper.WorkOrderServiceMapper;
import br.com.fiap.postech.soat16.fase1.model.Estimate;
import br.com.fiap.postech.soat16.fase1.model.EstimateItem;
import br.com.fiap.postech.soat16.fase1.model.EstimateStatus;
import br.com.fiap.postech.soat16.fase1.model.WorkOrder;
import br.com.fiap.postech.soat16.fase1.model.WorkOrderHistory;
import br.com.fiap.postech.soat16.fase1.model.WorkOrderStatus;
import br.com.fiap.postech.soat16.fase1.repository.CustomerRepository;
import br.com.fiap.postech.soat16.fase1.repository.EstimateRepository;
import br.com.fiap.postech.soat16.fase1.repository.PartRepository;
import br.com.fiap.postech.soat16.fase1.repository.VehicleRepository;
import br.com.fiap.postech.soat16.fase1.repository.WorkOrderHistoryRepository;
import br.com.fiap.postech.soat16.fase1.repository.WorkOrderRepository;
import br.com.fiap.postech.soat16.fase1.repository.WorkOrderServiceRepository;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class WorkOrderService {

    private static final Map<WorkOrderStatus, WorkOrderStatus> FORWARD_TRANSITIONS = Map.of(
            WorkOrderStatus.OPEN, WorkOrderStatus.DIAGNOSIS,
            WorkOrderStatus.DIAGNOSIS, WorkOrderStatus.WAITING_APPROVAL,
            WorkOrderStatus.WAITING_APPROVAL, WorkOrderStatus.APPROVED,
            WorkOrderStatus.APPROVED, WorkOrderStatus.IN_PROGRESS
    );

    private final WorkOrderRepository repository;
    private final WorkOrderHistoryRepository historyRepository;
    private final EstimateRepository estimateRepository;
    private final WorkOrderServiceRepository serviceRepository;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final PartRepository partRepository;
    private final WorkOrderMapper mapper;
    private final EstimateMapper estimateMapper;
    private final WorkOrderServiceMapper serviceMapper;

    @WithSession
    public Uni<PageableResponseDto<WorkOrderResponseDto>> findAll(String q, int page, int size) {
        return Uni.combine().all()
                .unis(repository.findPage(page, size), repository.count())
                .asTuple()
                .map(tuple -> {
                    var data = tuple.getItem1().stream().map(mapper::toResponse).toList();
                    return PageableResponseDto.of(data, page, size, tuple.getItem2());
                });
    }

    @WithSession
    public Uni<WorkOrderResponseDto> findById(UUID id) {
        return repository.findByWorkOrderId(id)
                .onItem().ifNull().failWith(WorkOrderNotFoundException::new)
                .map(mapper::toResponse);
    }

    @WithTransaction
    public Uni<Void> create(WorkOrderRequestDto request) {
        return customerRepository.findByCustomerId(request.customerId())
                .onItem().ifNull().failWith(CustomerNotFoundException::new)
                .flatMap(customer -> vehicleRepository.findByVehicleId(request.vehicleId())
                        .onItem().ifNull().failWith(VehicleNotFoundException::new)
                        .flatMap(vehicle -> repository.persist(mapper.toEntity(request, customer, vehicle))
                                .replaceWithVoid()));
    }

    @WithTransaction
    public Uni<WorkOrderResponseDto> updateStatus(UUID id, WorkOrderStatusUpdateRequestDto request) {
        return repository.findByWorkOrderId(id)
                .onItem().ifNull().failWith(WorkOrderNotFoundException::new)
                .flatMap(order -> assertNotLocked(order)
                        .flatMap(v -> validateGenericTransition(order.getStatus(), request.status()))
                        .flatMap(v -> request.status() == WorkOrderStatus.APPROVED
                                || request.status() == WorkOrderStatus.IN_PROGRESS
                                ? assertHasApprovedEstimate(id)
                                : Uni.createFrom().voidItem())
                        .flatMap(v -> changeStatus(order, request.status()))
                        .flatMap(repository::persist)
                        .map(mapper::toResponse));
    }

    @WithTransaction
    public Uni<EstimateResponseDto> createEstimate(UUID workOrderId, EstimateRequestDto request) {
        return repository.findByWorkOrderId(workOrderId)
                .onItem().ifNull().failWith(WorkOrderNotFoundException::new)
                .flatMap(order -> assertNotLocked(order)
                        .flatMap(v -> buildItems(request))
                        .flatMap(items -> persistEstimate(order, items)))
                .map(estimateMapper::toResponse);
    }

    @WithTransaction
    public Uni<EstimateResponseDto> approveEstimate(UUID workOrderId, UUID estimateId) {
        return repository.findByWorkOrderId(workOrderId)
                .onItem().ifNull().failWith(WorkOrderNotFoundException::new)
                .flatMap(order -> assertNotLocked(order)
                        .flatMap(v -> estimateRepository.findByEstimateIdAndWorkOrderId(estimateId, workOrderId))
                        .onItem().ifNull().failWith(EstimateNotFoundException::new)
                        .flatMap(estimate -> estimate.getStatus() == EstimateStatus.PENDING
                                ? approve(order, estimate)
                                : Uni.createFrom().<Estimate>failure(new EstimateAlreadyDecidedException())))
                .map(estimateMapper::toResponse);
    }

    @WithTransaction
    public Uni<WorkOrderServiceResponseDto> addService(UUID workOrderId, WorkOrderServiceRequestDto request) {
        return repository.findByWorkOrderId(workOrderId)
                .onItem().ifNull().failWith(WorkOrderNotFoundException::new)
                .flatMap(order -> assertNotLocked(order)
                        .flatMap(v -> serviceRepository.persist(serviceMapper.toEntity(request, order))))
                .map(serviceMapper::toResponse);
    }

    @WithTransaction
    public Uni<WorkOrderResponseDto> close(UUID id, WorkOrderCloseRequestDto request) {
        return repository.findByWorkOrderId(id)
                .onItem().ifNull().failWith(WorkOrderNotFoundException::new)
                .flatMap(order -> assertNotLocked(order)
                        .flatMap(v -> order.getStatus() == WorkOrderStatus.IN_PROGRESS
                                ? Uni.createFrom().<Void>voidItem()
                                : Uni.createFrom().<Void>failure(new InvalidWorkOrderStatusTransitionException(
                                        "Only work orders IN_PROGRESS can be closed")))
                        .flatMap(v -> assertHasApprovedEstimate(id))
                        .flatMap(v -> {
                            order.setFinalValue(request.finalValue() != null
                                    ? request.finalValue() : order.getEstimatedValue());
                            order.setClosedAt(LocalDateTime.now());
                            return changeStatus(order, WorkOrderStatus.COMPLETED);
                        })
                        .flatMap(repository::persist)
                        .map(mapper::toResponse));
    }

    private Uni<Estimate> approve(WorkOrder order, Estimate estimate) {
        estimate.setStatus(EstimateStatus.APPROVED);
        estimate.setApprovedAt(LocalDateTime.now());
        order.setEstimatedValue(estimate.getTotalAmount());
        Uni<WorkOrder> orderUni = order.getStatus() == WorkOrderStatus.WAITING_APPROVAL
                ? changeStatus(order, WorkOrderStatus.APPROVED).flatMap(repository::persist)
                : repository.persist(order);
        return orderUni.flatMap(persisted -> estimateRepository.persist(estimate));
    }

    private Uni<List<EstimateItem>> buildItems(EstimateRequestDto request) {
        List<Uni<EstimateItem>> itemUnis = request.items().stream()
                .map(itemDto -> partRepository.find("id = ?1", itemDto.partId()).firstResult()
                        .onItem().ifNull().failWith(() -> new EstimatePartNotFoundException(itemDto.partId()))
                        .map(part -> estimateMapper.toItemEntity(itemDto, part)))
                .toList();
        return Uni.join().all(itemUnis).andFailFast();
    }

    private Uni<Estimate> persistEstimate(WorkOrder order, List<EstimateItem> items) {
        var totalAmount = items.stream().map(EstimateItem::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        var estimate = new Estimate();
        estimate.setWorkOrder(order);
        estimate.setStatus(EstimateStatus.PENDING);
        estimate.setTotalAmount(totalAmount);
        estimate.setItems(items);
        items.forEach(item -> item.setEstimate(estimate));
        return estimateRepository.persist(estimate);
    }

    private Uni<WorkOrder> changeStatus(WorkOrder order, WorkOrderStatus newStatus) {
        var history = new WorkOrderHistory();
        history.setWorkOrder(order);
        history.setPreviousStatus(order.getStatus());
        history.setNewStatus(newStatus);
        history.setChangedAt(LocalDateTime.now());
        order.setStatus(newStatus);
        return historyRepository.persist(history).replaceWith(order);
    }

    private Uni<Void> assertNotLocked(WorkOrder order) {
        if (order.getStatus() == WorkOrderStatus.DELIVERED || order.getStatus() == WorkOrderStatus.CANCELLED) {
            return Uni.createFrom().failure(new WorkOrderLockedException());
        }
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> validateGenericTransition(WorkOrderStatus current, WorkOrderStatus target) {
        if (target == WorkOrderStatus.COMPLETED) {
            return Uni.createFrom().failure(new InvalidWorkOrderStatusTransitionException(
                    "Use PATCH /v1/work-orders/{id}/close to complete a work order"));
        }
        if (target == WorkOrderStatus.CANCELLED) {
            return Uni.createFrom().voidItem();
        }
        if (target == WorkOrderStatus.DELIVERED) {
            return current == WorkOrderStatus.COMPLETED
                    ? Uni.createFrom().voidItem()
                    : Uni.createFrom().failure(new InvalidWorkOrderStatusTransitionException(current, target));
        }
        return target.equals(FORWARD_TRANSITIONS.get(current))
                ? Uni.createFrom().voidItem()
                : Uni.createFrom().failure(new InvalidWorkOrderStatusTransitionException(current, target));
    }

    private Uni<Void> assertHasApprovedEstimate(UUID workOrderId) {
        return estimateRepository.existsApprovedByWorkOrderId(workOrderId)
                .flatMap(exists -> Boolean.TRUE.equals(exists)
                        ? Uni.createFrom().voidItem()
                        : Uni.createFrom().failure(new EstimateNotApprovedException()));
    }
}
