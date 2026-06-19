package br.com.fiap.postech.soat16.fase1.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.request.EstimateItemRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.EstimateRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.WorkOrderCloseRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.WorkOrderRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.WorkOrderServiceRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.WorkOrderStatusUpdateRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.EstimateResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkOrderResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkOrderServiceResponseDto;
import br.com.fiap.postech.soat16.fase1.exception.BusinessException;
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
import br.com.fiap.postech.soat16.fase1.model.Customer;
import br.com.fiap.postech.soat16.fase1.model.Estimate;
import br.com.fiap.postech.soat16.fase1.model.EstimateItem;
import br.com.fiap.postech.soat16.fase1.model.EstimateStatus;
import br.com.fiap.postech.soat16.fase1.model.Part;
import br.com.fiap.postech.soat16.fase1.model.Vehicle;
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

import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkOrderService — Unit Tests")
class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository repository;

    @Mock
    private WorkOrderHistoryRepository historyRepository;

    @Mock
    private EstimateRepository estimateRepository;

    @Mock
    private WorkOrderServiceRepository serviceRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private PartRepository partRepository;

    @Mock
    private WorkOrderMapper mapper;

    @Mock
    private EstimateMapper estimateMapper;

    @Mock
    private WorkOrderServiceMapper serviceMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PanacheQuery<Part> partQuery;

    private WorkOrderService service;

    private static final UUID WORK_ORDER_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID VEHICLE_ID = UUID.randomUUID();

    private WorkOrder entity;
    private WorkOrderResponseDto response;

    @BeforeEach
    void setUp() {
        service = new WorkOrderService(repository, historyRepository, estimateRepository, serviceRepository,
                customerRepository, vehicleRepository, partRepository, mapper, estimateMapper, serviceMapper,
                notificationService);

        lenient().when(notificationService.notifyEstimateReady(any(), any()))
                .thenReturn(Uni.createFrom().voidItem());
        lenient().when(notificationService.notifyWorkOrderCompleted(any()))
                .thenReturn(Uni.createFrom().voidItem());

        entity = new WorkOrder();
        entity.setId(WORK_ORDER_ID);
        entity.setStatus(WorkOrderStatus.OPEN);

        response = new WorkOrderResponseDto(WORK_ORDER_ID, CUSTOMER_ID, VEHICLE_ID, "desc", null,
                WorkOrderStatus.OPEN, null, null, null, null);
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return paginated response")
        void shouldReturnPaginatedResponse() {
            when(repository.findPage(0, 10)).thenReturn(Uni.createFrom().item(List.of(entity)));
            when(repository.count()).thenReturn(Uni.createFrom().item(1L));
            when(mapper.toResponse(entity)).thenReturn(response);

            PageableResponseDto<WorkOrderResponseDto> result = service.findAll(null, 0, 10).await().indefinitely();

            assertNotNull(result);
            assertEquals(1, result.content().size());
            assertEquals(1L, result.pagination().totalElements());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return work order response when found")
        void shouldReturnWhenFound() {
            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(mapper.toResponse(entity)).thenReturn(response);

            WorkOrderResponseDto result = service.findById(WORK_ORDER_ID).await().indefinitely();

            assertEquals(WORK_ORDER_ID, result.workOrderId());
        }

        @Test
        @DisplayName("should throw WorkOrderNotFoundException when not found")
        void shouldThrowWhenMissing() {
            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().nullItem());

            assertThrows(WorkOrderNotFoundException.class,
                    () -> service.findById(WORK_ORDER_ID).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should persist work order when customer and vehicle exist")
        void shouldPersistWhenCustomerAndVehicleExist() {
            Customer customer = new Customer();
            Vehicle vehicle = new Vehicle();
            WorkOrderRequestDto request = new WorkOrderRequestDto(CUSTOMER_ID, VEHICLE_ID, "desc", null);

            when(customerRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Uni.createFrom().item(customer));
            when(vehicleRepository.findByVehicleId(VEHICLE_ID)).thenReturn(Uni.createFrom().item(vehicle));
            when(mapper.toEntity(request, customer, vehicle)).thenReturn(entity);
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));

            assertDoesNotThrow(() -> service.create(request).await().indefinitely());
            verify(repository).persist(entity);
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when customer does not exist")
        void shouldThrowWhenCustomerMissing() {
            WorkOrderRequestDto request = new WorkOrderRequestDto(CUSTOMER_ID, VEHICLE_ID, "desc", null);
            when(customerRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Uni.createFrom().nullItem());

            assertThrows(CustomerNotFoundException.class,
                    () -> service.create(request).await().indefinitely());
            verify(repository, never()).persist(any(WorkOrder.class));
        }

        @Test
        @DisplayName("should throw VehicleNotFoundException when vehicle does not exist")
        void shouldThrowWhenVehicleMissing() {
            Customer customer = new Customer();
            WorkOrderRequestDto request = new WorkOrderRequestDto(CUSTOMER_ID, VEHICLE_ID, "desc", null);

            when(customerRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Uni.createFrom().item(customer));
            when(vehicleRepository.findByVehicleId(VEHICLE_ID)).thenReturn(Uni.createFrom().nullItem());

            assertThrows(VehicleNotFoundException.class,
                    () -> service.create(request).await().indefinitely());
            verify(repository, never()).persist(any(WorkOrder.class));
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("should move to the next status and record history")
        void shouldMoveToNextStatusAndRecordHistory() {
            entity.setStatus(WorkOrderStatus.OPEN);
            WorkOrderStatusUpdateRequestDto request = new WorkOrderStatusUpdateRequestDto(WorkOrderStatus.DIAGNOSIS);

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(historyRepository.persist(any(WorkOrderHistory.class)))
                    .thenReturn(Uni.createFrom().item(new WorkOrderHistory()));
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));
            when(mapper.toResponse(entity)).thenReturn(response);

            service.updateStatus(WORK_ORDER_ID, request).await().indefinitely();

            assertEquals(WorkOrderStatus.DIAGNOSIS, entity.getStatus());
            verify(historyRepository).persist(any(WorkOrderHistory.class));
        }

        @Test
        @DisplayName("should reject jumping directly to COMPLETED")
        void shouldRejectCompletedViaGenericEndpoint() {
            entity.setStatus(WorkOrderStatus.IN_PROGRESS);
            WorkOrderStatusUpdateRequestDto request = new WorkOrderStatusUpdateRequestDto(WorkOrderStatus.COMPLETED);
            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));

            assertThrows(InvalidWorkOrderStatusTransitionException.class,
                    () -> service.updateStatus(WORK_ORDER_ID, request).await().indefinitely());
        }

        @Test
        @DisplayName("should reject skipping stages")
        void shouldRejectSkippingStages() {
            entity.setStatus(WorkOrderStatus.OPEN);
            WorkOrderStatusUpdateRequestDto request = new WorkOrderStatusUpdateRequestDto(WorkOrderStatus.APPROVED);
            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));

            assertThrows(InvalidWorkOrderStatusTransitionException.class,
                    () -> service.updateStatus(WORK_ORDER_ID, request).await().indefinitely());
        }

        @Test
        @DisplayName("should allow CANCELLED from any non-terminal status")
        void shouldAllowCancelledFromAnyNonTerminalStatus() {
            entity.setStatus(WorkOrderStatus.DIAGNOSIS);
            WorkOrderStatusUpdateRequestDto request = new WorkOrderStatusUpdateRequestDto(WorkOrderStatus.CANCELLED);

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(historyRepository.persist(any(WorkOrderHistory.class)))
                    .thenReturn(Uni.createFrom().item(new WorkOrderHistory()));
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));
            when(mapper.toResponse(entity)).thenReturn(response);

            service.updateStatus(WORK_ORDER_ID, request).await().indefinitely();

            assertEquals(WorkOrderStatus.CANCELLED, entity.getStatus());
        }

        @Test
        @DisplayName("should reject DELIVERED unless current status is COMPLETED")
        void shouldRejectDeliveredFromNonCompleted() {
            entity.setStatus(WorkOrderStatus.IN_PROGRESS);
            WorkOrderStatusUpdateRequestDto request = new WorkOrderStatusUpdateRequestDto(WorkOrderStatus.DELIVERED);
            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));

            assertThrows(InvalidWorkOrderStatusTransitionException.class,
                    () -> service.updateStatus(WORK_ORDER_ID, request).await().indefinitely());
        }

        @Test
        @DisplayName("should allow DELIVERED from COMPLETED")
        void shouldAllowDeliveredFromCompleted() {
            entity.setStatus(WorkOrderStatus.COMPLETED);
            WorkOrderStatusUpdateRequestDto request = new WorkOrderStatusUpdateRequestDto(WorkOrderStatus.DELIVERED);

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(historyRepository.persist(any(WorkOrderHistory.class)))
                    .thenReturn(Uni.createFrom().item(new WorkOrderHistory()));
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));
            when(mapper.toResponse(entity)).thenReturn(response);

            service.updateStatus(WORK_ORDER_ID, request).await().indefinitely();

            assertEquals(WorkOrderStatus.DELIVERED, entity.getStatus());
        }

        @Test
        @DisplayName("should throw WorkOrderLockedException when current status is DELIVERED")
        void shouldThrowLockedWhenDelivered() {
            entity.setStatus(WorkOrderStatus.DELIVERED);
            WorkOrderStatusUpdateRequestDto request = new WorkOrderStatusUpdateRequestDto(WorkOrderStatus.CANCELLED);
            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));

            assertThrows(WorkOrderLockedException.class,
                    () -> service.updateStatus(WORK_ORDER_ID, request).await().indefinitely());
        }

        @Test
        @DisplayName("should throw WorkOrderLockedException when current status is CANCELLED")
        void shouldThrowLockedWhenCancelled() {
            entity.setStatus(WorkOrderStatus.CANCELLED);
            WorkOrderStatusUpdateRequestDto request = new WorkOrderStatusUpdateRequestDto(WorkOrderStatus.DIAGNOSIS);
            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));

            assertThrows(WorkOrderLockedException.class,
                    () -> service.updateStatus(WORK_ORDER_ID, request).await().indefinitely());
        }

        @Test
        @DisplayName("should throw EstimateNotApprovedException when starting execution without approved estimate")
        void shouldThrowWhenStartingWithoutApprovedEstimate() {
            entity.setStatus(WorkOrderStatus.APPROVED);
            WorkOrderStatusUpdateRequestDto request = new WorkOrderStatusUpdateRequestDto(WorkOrderStatus.IN_PROGRESS);

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(estimateRepository.existsApprovedByWorkOrderId(WORK_ORDER_ID))
                    .thenReturn(Uni.createFrom().item(false));

            assertThrows(EstimateNotApprovedException.class,
                    () -> service.updateStatus(WORK_ORDER_ID, request).await().indefinitely());
        }

        @Test
        @DisplayName("should allow starting execution when an approved estimate exists")
        void shouldAllowStartingWithApprovedEstimate() {
            entity.setStatus(WorkOrderStatus.APPROVED);
            WorkOrderStatusUpdateRequestDto request = new WorkOrderStatusUpdateRequestDto(WorkOrderStatus.IN_PROGRESS);

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(estimateRepository.existsApprovedByWorkOrderId(WORK_ORDER_ID))
                    .thenReturn(Uni.createFrom().item(true));
            when(historyRepository.persist(any(WorkOrderHistory.class)))
                    .thenReturn(Uni.createFrom().item(new WorkOrderHistory()));
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));
            when(mapper.toResponse(entity)).thenReturn(response);

            service.updateStatus(WORK_ORDER_ID, request).await().indefinitely();

            assertEquals(WorkOrderStatus.IN_PROGRESS, entity.getStatus());
        }

        @Test
        @DisplayName("should throw EstimateNotApprovedException when approving without an approved estimate")
        void shouldThrowWhenApprovingWithoutApprovedEstimate() {
            entity.setStatus(WorkOrderStatus.WAITING_APPROVAL);
            WorkOrderStatusUpdateRequestDto request = new WorkOrderStatusUpdateRequestDto(WorkOrderStatus.APPROVED);

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(estimateRepository.existsApprovedByWorkOrderId(WORK_ORDER_ID))
                    .thenReturn(Uni.createFrom().item(false));

            assertThrows(EstimateNotApprovedException.class,
                    () -> service.updateStatus(WORK_ORDER_ID, request).await().indefinitely());
        }

        @Test
        @DisplayName("should allow approving when an approved estimate exists")
        void shouldAllowApprovingWithApprovedEstimate() {
            entity.setStatus(WorkOrderStatus.WAITING_APPROVAL);
            WorkOrderStatusUpdateRequestDto request = new WorkOrderStatusUpdateRequestDto(WorkOrderStatus.APPROVED);

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(estimateRepository.existsApprovedByWorkOrderId(WORK_ORDER_ID))
                    .thenReturn(Uni.createFrom().item(true));
            when(historyRepository.persist(any(WorkOrderHistory.class)))
                    .thenReturn(Uni.createFrom().item(new WorkOrderHistory()));
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));
            when(mapper.toResponse(entity)).thenReturn(response);

            service.updateStatus(WORK_ORDER_ID, request).await().indefinitely();

            assertEquals(WorkOrderStatus.APPROVED, entity.getStatus());
        }

        @Test
        @DisplayName("should throw WorkOrderNotFoundException when work order does not exist")
        void shouldThrowWhenMissing() {
            WorkOrderStatusUpdateRequestDto request = new WorkOrderStatusUpdateRequestDto(WorkOrderStatus.DIAGNOSIS);
            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().nullItem());

            assertThrows(WorkOrderNotFoundException.class,
                    () -> service.updateStatus(WORK_ORDER_ID, request).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("createEstimate")
    class CreateEstimate {

        @Test
        @DisplayName("should build items, compute totalAmount and persist the estimate")
        void shouldBuildAndPersistEstimate() {
            UUID partId = UUID.randomUUID();
            Part part = new Part("Filtro", "desc", BigDecimal.TEN, 5, "UN");
            EstimateItemRequestDto itemDto = new EstimateItemRequestDto(partId, 2, BigDecimal.TEN);
            EstimateRequestDto request = new EstimateRequestDto(List.of(itemDto));

            EstimateItem item = new EstimateItem();
            item.setPart(part);
            item.setQuantity(2);
            item.setUnitPrice(BigDecimal.TEN);
            item.setTotalPrice(BigDecimal.valueOf(20));

            Estimate persistedEstimate = new Estimate();
            EstimateResponseDto estimateResponse = new EstimateResponseDto(
                    UUID.randomUUID(), WORK_ORDER_ID, EstimateStatus.PENDING, BigDecimal.valueOf(20), BigDecimal.ZERO,
                    BigDecimal.valueOf(20), null, null, List.of());

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(partRepository.find("id = ?1", partId)).thenReturn(partQuery);
            when(partQuery.firstResult()).thenReturn(Uni.createFrom().item(part));
            when(estimateMapper.toItemEntity(itemDto, part)).thenReturn(item);
            when(serviceRepository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(List.of()));
            when(estimateRepository.persist(any(Estimate.class))).thenReturn(Uni.createFrom().item(persistedEstimate));
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));
            when(estimateMapper.toResponse(persistedEstimate)).thenReturn(estimateResponse);

            EstimateResponseDto result = service.createEstimate(WORK_ORDER_ID, request).await().indefinitely();

            assertEquals(BigDecimal.valueOf(20), result.totalAmount());
            verify(estimateRepository).persist(any(Estimate.class));
        }

        @Test
        @DisplayName("should throw EstimatePartNotFoundException when a referenced part does not exist")
        void shouldThrowWhenPartMissing() {
            UUID partId = UUID.randomUUID();
            EstimateItemRequestDto itemDto = new EstimateItemRequestDto(partId, 2, BigDecimal.TEN);
            EstimateRequestDto request = new EstimateRequestDto(List.of(itemDto));

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(partRepository.find("id = ?1", partId)).thenReturn(partQuery);
            when(partQuery.firstResult()).thenReturn(Uni.createFrom().nullItem());

            assertThrows(EstimatePartNotFoundException.class,
                    () -> service.createEstimate(WORK_ORDER_ID, request).await().indefinitely());
        }

        @Test
        @DisplayName("should throw WorkOrderLockedException when work order is delivered or cancelled")
        void shouldThrowWhenLocked() {
            entity.setStatus(WorkOrderStatus.CANCELLED);
            EstimateRequestDto request = new EstimateRequestDto(
                    List.of(new EstimateItemRequestDto(UUID.randomUUID(), 1, BigDecimal.ONE)));
            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));

            assertThrows(WorkOrderLockedException.class,
                    () -> service.createEstimate(WORK_ORDER_ID, request).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("approveEstimate")
    class ApproveEstimate {

        @Test
        @DisplayName("should approve estimate, set estimatedValue and advance WAITING_APPROVAL to APPROVED")
        void shouldApproveAndAdvanceStatus() {
            entity.setStatus(WorkOrderStatus.WAITING_APPROVAL);
            UUID estimateId = UUID.randomUUID();
            Estimate estimate = new Estimate();
            estimate.setId(estimateId);
            estimate.setWorkOrder(entity);
            estimate.setStatus(EstimateStatus.PENDING);
            estimate.setTotalAmount(BigDecimal.valueOf(100));

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(estimateRepository.findByEstimateIdAndWorkOrderId(estimateId, WORK_ORDER_ID))
                    .thenReturn(Uni.createFrom().item(estimate));
            when(historyRepository.persist(any(WorkOrderHistory.class)))
                    .thenReturn(Uni.createFrom().item(new WorkOrderHistory()));
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));
            when(estimateRepository.persist(estimate)).thenReturn(Uni.createFrom().item(estimate));
            when(estimateMapper.toResponse(estimate)).thenReturn(new EstimateResponseDto(
                    estimateId, WORK_ORDER_ID, EstimateStatus.APPROVED, BigDecimal.valueOf(100), BigDecimal.ZERO,
                    BigDecimal.valueOf(100), null, null, List.of()));

            service.approveEstimate(WORK_ORDER_ID, estimateId).await().indefinitely();

            assertEquals(EstimateStatus.APPROVED, estimate.getStatus());
            assertEquals(WorkOrderStatus.APPROVED, entity.getStatus());
            assertEquals(BigDecimal.valueOf(100), entity.getEstimatedValue());
        }

        @Test
        @DisplayName("should approve estimate without forcing a status change when not WAITING_APPROVAL")
        void shouldApproveWithoutForcingStatusChange() {
            entity.setStatus(WorkOrderStatus.IN_PROGRESS);
            UUID estimateId = UUID.randomUUID();
            Estimate estimate = new Estimate();
            estimate.setId(estimateId);
            estimate.setWorkOrder(entity);
            estimate.setStatus(EstimateStatus.PENDING);
            estimate.setTotalAmount(BigDecimal.valueOf(50));

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(estimateRepository.findByEstimateIdAndWorkOrderId(estimateId, WORK_ORDER_ID))
                    .thenReturn(Uni.createFrom().item(estimate));
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));
            when(estimateRepository.persist(estimate)).thenReturn(Uni.createFrom().item(estimate));
            when(estimateMapper.toResponse(estimate)).thenReturn(new EstimateResponseDto(
                    estimateId, WORK_ORDER_ID, EstimateStatus.APPROVED, BigDecimal.valueOf(50), BigDecimal.ZERO,
                    BigDecimal.valueOf(50), null, null, List.of()));

            service.approveEstimate(WORK_ORDER_ID, estimateId).await().indefinitely();

            assertEquals(WorkOrderStatus.IN_PROGRESS, entity.getStatus());
            verify(historyRepository, never()).persist(any(WorkOrderHistory.class));
        }

        @Test
        @DisplayName("should throw EstimateAlreadyDecidedException when estimate is not PENDING")
        void shouldThrowWhenAlreadyDecided() {
            UUID estimateId = UUID.randomUUID();
            Estimate estimate = new Estimate();
            estimate.setStatus(EstimateStatus.APPROVED);

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(estimateRepository.findByEstimateIdAndWorkOrderId(estimateId, WORK_ORDER_ID))
                    .thenReturn(Uni.createFrom().item(estimate));

            assertThrows(EstimateAlreadyDecidedException.class,
                    () -> service.approveEstimate(WORK_ORDER_ID, estimateId).await().indefinitely());
        }

        @Test
        @DisplayName("should throw EstimateNotFoundException when estimate does not belong to the work order")
        void shouldThrowWhenEstimateMissing() {
            UUID estimateId = UUID.randomUUID();
            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(estimateRepository.findByEstimateIdAndWorkOrderId(estimateId, WORK_ORDER_ID))
                    .thenReturn(Uni.createFrom().nullItem());

            assertThrows(EstimateNotFoundException.class,
                    () -> service.approveEstimate(WORK_ORDER_ID, estimateId).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("rejectEstimate")
    class RejectEstimate {

        @Test
        @DisplayName("should reject a pending estimate and move WAITING_APPROVAL back to DIAGNOSIS")
        void shouldRejectAndReturnToDiagnosis() {
            entity.setStatus(WorkOrderStatus.WAITING_APPROVAL);
            UUID estimateId = UUID.randomUUID();
            Estimate estimate = new Estimate();
            estimate.setId(estimateId);
            estimate.setWorkOrder(entity);
            estimate.setStatus(EstimateStatus.PENDING);
            estimate.setTotalAmount(BigDecimal.valueOf(100));

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(estimateRepository.findByEstimateIdAndWorkOrderId(estimateId, WORK_ORDER_ID))
                    .thenReturn(Uni.createFrom().item(estimate));
            when(historyRepository.persist(any(WorkOrderHistory.class)))
                    .thenReturn(Uni.createFrom().item(new WorkOrderHistory()));
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));
            when(estimateRepository.persist(estimate)).thenReturn(Uni.createFrom().item(estimate));
            when(estimateMapper.toResponse(estimate)).thenReturn(new EstimateResponseDto(
                    estimateId, WORK_ORDER_ID, EstimateStatus.REJECTED, BigDecimal.valueOf(100), BigDecimal.ZERO,
                    BigDecimal.valueOf(100), null, null, List.of()));

            service.rejectEstimate(WORK_ORDER_ID, estimateId).await().indefinitely();

            assertEquals(EstimateStatus.REJECTED, estimate.getStatus());
            assertEquals(WorkOrderStatus.DIAGNOSIS, entity.getStatus());
        }

        @Test
        @DisplayName("should throw EstimateAlreadyDecidedException when estimate is not PENDING")
        void shouldThrowWhenAlreadyDecided() {
            UUID estimateId = UUID.randomUUID();
            Estimate estimate = new Estimate();
            estimate.setStatus(EstimateStatus.APPROVED);

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(estimateRepository.findByEstimateIdAndWorkOrderId(estimateId, WORK_ORDER_ID))
                    .thenReturn(Uni.createFrom().item(estimate));

            assertThrows(EstimateAlreadyDecidedException.class,
                    () -> service.rejectEstimate(WORK_ORDER_ID, estimateId).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("stock management")
    class StockManagement {

        private Estimate estimateWithItem(Part part, int quantity) {
            EstimateItem item = new EstimateItem();
            item.setPart(part);
            item.setQuantity(quantity);
            item.setUnitPrice(part.getUnitPrice());
            item.setTotalPrice(part.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
            Estimate estimate = new Estimate();
            estimate.setWorkOrder(entity);
            estimate.setStatus(EstimateStatus.PENDING);
            estimate.setTotalAmount(item.getTotalPrice());
            estimate.setItems(new java.util.ArrayList<>(List.of(item)));
            return estimate;
        }

        @Test
        @DisplayName("should decrease part stock when approving the estimate")
        void shouldDecreaseStockOnApproval() {
            entity.setStatus(WorkOrderStatus.WAITING_APPROVAL);
            UUID estimateId = UUID.randomUUID();
            Part part = new Part("Filtro", "desc", BigDecimal.TEN, 5, "UN");
            Estimate estimate = estimateWithItem(part, 2);
            estimate.setId(estimateId);

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(estimateRepository.findByEstimateIdAndWorkOrderId(estimateId, WORK_ORDER_ID))
                    .thenReturn(Uni.createFrom().item(estimate));
            when(historyRepository.persist(any(WorkOrderHistory.class)))
                    .thenReturn(Uni.createFrom().item(new WorkOrderHistory()));
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));
            when(estimateRepository.persist(estimate)).thenReturn(Uni.createFrom().item(estimate));
            when(estimateMapper.toResponse(estimate)).thenReturn(new EstimateResponseDto(
                    estimateId, WORK_ORDER_ID, EstimateStatus.APPROVED, BigDecimal.valueOf(20), BigDecimal.ZERO,
                    BigDecimal.valueOf(20), null, null, List.of()));

            service.approveEstimate(WORK_ORDER_ID, estimateId).await().indefinitely();

            assertEquals(3, part.getStockQuantity());
        }

        @Test
        @DisplayName("should fail approval with insufficient stock and not change status")
        void shouldFailApprovalWhenInsufficientStock() {
            entity.setStatus(WorkOrderStatus.WAITING_APPROVAL);
            UUID estimateId = UUID.randomUUID();
            Part part = new Part("Filtro", "desc", BigDecimal.TEN, 1, "UN");
            Estimate estimate = estimateWithItem(part, 5);
            estimate.setId(estimateId);

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(estimateRepository.findByEstimateIdAndWorkOrderId(estimateId, WORK_ORDER_ID))
                    .thenReturn(Uni.createFrom().item(estimate));

            assertThrows(BusinessException.class,
                    () -> service.approveEstimate(WORK_ORDER_ID, estimateId).await().indefinitely());
            assertEquals(WorkOrderStatus.WAITING_APPROVAL, entity.getStatus());
            assertEquals(1, part.getStockQuantity());
        }

        @Test
        @DisplayName("should restore part stock when cancelling an APPROVED work order")
        void shouldRestoreStockOnCancel() {
            entity.setStatus(WorkOrderStatus.APPROVED);
            WorkOrderStatusUpdateRequestDto request = new WorkOrderStatusUpdateRequestDto(WorkOrderStatus.CANCELLED);
            Part part = new Part("Filtro", "desc", BigDecimal.TEN, 3, "UN");
            Estimate approvedEstimate = estimateWithItem(part, 2);
            approvedEstimate.setStatus(EstimateStatus.APPROVED);

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(estimateRepository.findApprovedByWorkOrderId(WORK_ORDER_ID))
                    .thenReturn(Uni.createFrom().item(approvedEstimate));
            when(historyRepository.persist(any(WorkOrderHistory.class)))
                    .thenReturn(Uni.createFrom().item(new WorkOrderHistory()));
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));
            when(mapper.toResponse(entity)).thenReturn(response);

            service.updateStatus(WORK_ORDER_ID, request).await().indefinitely();

            assertEquals(WorkOrderStatus.CANCELLED, entity.getStatus());
            assertEquals(5, part.getStockQuantity());
        }
    }

    @Nested
    @DisplayName("addService")
    class AddService {

        @Test
        @DisplayName("should persist a labor service line")
        void shouldPersistServiceLine() {
            WorkOrderServiceRequestDto request = new WorkOrderServiceRequestDto("Troca de oleo", BigDecimal.TEN);
            br.com.fiap.postech.soat16.fase1.model.WorkOrderService lineEntity =
                    new br.com.fiap.postech.soat16.fase1.model.WorkOrderService();
            WorkOrderServiceResponseDto lineResponse = new WorkOrderServiceResponseDto(
                    UUID.randomUUID(), WORK_ORDER_ID, "Troca de oleo", BigDecimal.TEN, null);

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(serviceMapper.toEntity(request, entity)).thenReturn(lineEntity);
            when(serviceRepository.persist(lineEntity)).thenReturn(Uni.createFrom().item(lineEntity));
            when(serviceMapper.toResponse(lineEntity)).thenReturn(lineResponse);

            WorkOrderServiceResponseDto result = service.addService(WORK_ORDER_ID, request).await().indefinitely();

            assertEquals("Troca de oleo", result.description());
            verify(serviceRepository, times(1)).persist(lineEntity);
        }

        @Test
        @DisplayName("should throw WorkOrderLockedException when work order is delivered or cancelled")
        void shouldThrowWhenLocked() {
            entity.setStatus(WorkOrderStatus.DELIVERED);
            WorkOrderServiceRequestDto request = new WorkOrderServiceRequestDto("Troca de oleo", BigDecimal.TEN);
            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));

            assertThrows(WorkOrderLockedException.class,
                    () -> service.addService(WORK_ORDER_ID, request).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("close")
    class Close {

        @Test
        @DisplayName("should complete the work order using the approved estimate value when finalValue is omitted")
        void shouldCompleteUsingEstimatedValueWhenOmitted() {
            entity.setStatus(WorkOrderStatus.IN_PROGRESS);
            entity.setEstimatedValue(BigDecimal.valueOf(200));
            WorkOrderCloseRequestDto request = new WorkOrderCloseRequestDto(null);

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(estimateRepository.existsApprovedByWorkOrderId(WORK_ORDER_ID))
                    .thenReturn(Uni.createFrom().item(true));
            when(historyRepository.persist(any(WorkOrderHistory.class)))
                    .thenReturn(Uni.createFrom().item(new WorkOrderHistory()));
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));
            when(mapper.toResponse(entity)).thenReturn(response);

            service.close(WORK_ORDER_ID, request).await().indefinitely();

            assertEquals(WorkOrderStatus.COMPLETED, entity.getStatus());
            assertEquals(BigDecimal.valueOf(200), entity.getFinalValue());
            assertNotNull(entity.getClosedAt());
        }

        @Test
        @DisplayName("should use the provided finalValue when present")
        void shouldUseProvidedFinalValue() {
            entity.setStatus(WorkOrderStatus.IN_PROGRESS);
            entity.setEstimatedValue(BigDecimal.valueOf(200));
            WorkOrderCloseRequestDto request = new WorkOrderCloseRequestDto(BigDecimal.valueOf(250));

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(estimateRepository.existsApprovedByWorkOrderId(WORK_ORDER_ID))
                    .thenReturn(Uni.createFrom().item(true));
            when(historyRepository.persist(any(WorkOrderHistory.class)))
                    .thenReturn(Uni.createFrom().item(new WorkOrderHistory()));
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));
            when(mapper.toResponse(entity)).thenReturn(response);

            service.close(WORK_ORDER_ID, request).await().indefinitely();

            assertEquals(BigDecimal.valueOf(250), entity.getFinalValue());
        }

        @Test
        @DisplayName("should record history transition to COMPLETED when finalValue differs from the estimate")
        void shouldRecordHistoryWhenFinalValueDiffersFromEstimate() {
            entity.setStatus(WorkOrderStatus.IN_PROGRESS);
            entity.setEstimatedValue(BigDecimal.valueOf(200));
            WorkOrderCloseRequestDto request = new WorkOrderCloseRequestDto(BigDecimal.valueOf(250));

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(estimateRepository.existsApprovedByWorkOrderId(WORK_ORDER_ID))
                    .thenReturn(Uni.createFrom().item(true));
            when(historyRepository.persist(any(WorkOrderHistory.class)))
                    .thenReturn(Uni.createFrom().item(new WorkOrderHistory()));
            when(repository.persist(entity)).thenReturn(Uni.createFrom().item(entity));
            when(mapper.toResponse(entity)).thenReturn(response);

            service.close(WORK_ORDER_ID, request).await().indefinitely();

            ArgumentCaptor<WorkOrderHistory> historyCaptor = ArgumentCaptor.forClass(WorkOrderHistory.class);
            verify(historyRepository).persist(historyCaptor.capture());
            WorkOrderHistory history = historyCaptor.getValue();
            assertEquals(WorkOrderStatus.IN_PROGRESS, history.getPreviousStatus());
            assertEquals(WorkOrderStatus.COMPLETED, history.getNewStatus());
            assertEquals(BigDecimal.valueOf(250), entity.getFinalValue());
        }

        @Test
        @DisplayName("should throw InvalidWorkOrderStatusTransitionException when not IN_PROGRESS")
        void shouldThrowWhenNotInProgress() {
            entity.setStatus(WorkOrderStatus.DIAGNOSIS);
            WorkOrderCloseRequestDto request = new WorkOrderCloseRequestDto(null);
            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));

            assertThrows(InvalidWorkOrderStatusTransitionException.class,
                    () -> service.close(WORK_ORDER_ID, request).await().indefinitely());
        }

        @Test
        @DisplayName("should throw EstimateNotApprovedException when there is no approved estimate")
        void shouldThrowWhenNoApprovedEstimate() {
            entity.setStatus(WorkOrderStatus.IN_PROGRESS);
            WorkOrderCloseRequestDto request = new WorkOrderCloseRequestDto(null);

            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));
            when(estimateRepository.existsApprovedByWorkOrderId(WORK_ORDER_ID))
                    .thenReturn(Uni.createFrom().item(false));

            assertThrows(EstimateNotApprovedException.class,
                    () -> service.close(WORK_ORDER_ID, request).await().indefinitely());
        }

        @Test
        @DisplayName("should throw WorkOrderLockedException when already delivered")
        void shouldThrowWhenLocked() {
            entity.setStatus(WorkOrderStatus.DELIVERED);
            WorkOrderCloseRequestDto request = new WorkOrderCloseRequestDto(null);
            when(repository.findByWorkOrderId(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(entity));

            assertThrows(WorkOrderLockedException.class,
                    () -> service.close(WORK_ORDER_ID, request).await().indefinitely());
        }
    }
}
