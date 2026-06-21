package br.com.fiap.postech.soat16.fase1.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.pagination.PageableResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.pagination.PaginationDto;
import br.com.fiap.postech.soat16.fase1.dto.request.EstimateItemRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.EstimateRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.WorkOrderCloseRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.WorkOrderRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.WorkOrderServiceRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.request.WorkOrderStatusUpdateRequestDto;
import br.com.fiap.postech.soat16.fase1.dto.response.EstimateResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkOrderMetricsResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkOrderResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkOrderServiceResponseDto;
import br.com.fiap.postech.soat16.fase1.exception.EstimateNotApprovedException;
import br.com.fiap.postech.soat16.fase1.exception.WorkOrderLockedException;
import br.com.fiap.postech.soat16.fase1.exception.WorkOrderNotFoundException;
import br.com.fiap.postech.soat16.fase1.model.EstimateStatus;
import br.com.fiap.postech.soat16.fase1.model.WorkOrderPriority;
import br.com.fiap.postech.soat16.fase1.model.WorkOrderStatus;
import br.com.fiap.postech.soat16.fase1.service.WorkOrderService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkOrderController — Unit Tests")
class WorkOrderControllerTest {

    @Mock
    private WorkOrderService service;

    private WorkOrderController controller;

    private static final UUID FIXED_UUID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    private WorkOrderResponseDto response;

    @BeforeEach
    void setUp() {
        controller = new WorkOrderController(service);
        response = new WorkOrderResponseDto(FIXED_UUID, UUID.randomUUID(), UUID.randomUUID(), "desc",
                WorkOrderPriority.MEDIUM, WorkOrderStatus.RECEIVED, null, null, null, null);
    }

    @Nested
    @DisplayName("GET /v1/work-orders — findAll")
    class FindAll {

        @Test
        @DisplayName("should return paginated list when work orders exist")
        void shouldReturnPaginatedList() {
            PageableRequestDto pageable = mock(PageableRequestDto.class);
            when(pageable.getQ()).thenReturn(null);
            when(pageable.getPage()).thenReturn(0);
            when(pageable.getSize()).thenReturn(10);

            PaginationDto paginationDto = new PaginationDto(0, 10, 1L, 1, false, false);
            PageableResponseDto<WorkOrderResponseDto> page = new PageableResponseDto<>(List.of(response), paginationDto);
            when(service.findAll(null, 0, 10)).thenReturn(Uni.createFrom().item(page));

            PageableResponseDto<WorkOrderResponseDto> result = controller.findAll(pageable).await().indefinitely();

            assertEquals(1, result.content().size());
            verify(service).findAll(null, 0, 10);
        }
    }

    @Nested
    @DisplayName("GET /v1/work-orders/{id} — findById")
    class FindById {

        @Test
        @DisplayName("should return work order when found")
        void shouldReturnWhenFound() {
            when(service.findById(FIXED_UUID)).thenReturn(Uni.createFrom().item(response));

            WorkOrderResponseDto result = controller.findById(FIXED_UUID).await().indefinitely();

            assertNotNull(result);
            verify(service).findById(FIXED_UUID);
        }

        @Test
        @DisplayName("should propagate WorkOrderNotFoundException")
        void shouldPropagateNotFoundException() {
            when(service.findById(FIXED_UUID)).thenReturn(Uni.createFrom().failure(new WorkOrderNotFoundException()));

            assertThrows(WorkOrderNotFoundException.class,
                    () -> controller.findById(FIXED_UUID).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("GET /v1/work-orders/metrics/average-execution-time — averageExecutionTime")
    class AverageExecutionTime {

        @Test
        @DisplayName("should return the average execution metrics")
        void shouldReturnMetrics() {
            WorkOrderMetricsResponseDto metrics = new WorkOrderMetricsResponseDto(3, 45.5);
            when(service.averageExecutionTime()).thenReturn(Uni.createFrom().item(metrics));

            WorkOrderMetricsResponseDto result = controller.averageExecutionTime().await().indefinitely();

            assertEquals(3, result.completedWorkOrders());
            assertEquals(45.5, result.averageExecutionMinutes());
            verify(service).averageExecutionTime();
        }
    }

    @Nested
    @DisplayName("POST /v1/work-orders — create")
    class Create {

        @Test
        @DisplayName("should return HTTP 201 when create succeeds")
        void shouldReturn201WhenCreateSucceeds() {
            WorkOrderRequestDto dto = new WorkOrderRequestDto(UUID.randomUUID(), UUID.randomUUID(), "desc", null);
            when(service.create(dto)).thenReturn(Uni.createFrom().voidItem());

            Response result = controller.create(dto).await().indefinitely();

            assertEquals(201, result.getStatus());
            verify(service).create(dto);
        }
    }

    @Nested
    @DisplayName("PATCH /v1/work-orders/{id}/status — updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("should return updated work order")
        void shouldReturnUpdatedWorkOrder() {
            WorkOrderStatusUpdateRequestDto dto = new WorkOrderStatusUpdateRequestDto(WorkOrderStatus.DIAGNOSIS);
            when(service.updateStatus(FIXED_UUID, dto)).thenReturn(Uni.createFrom().item(response));

            WorkOrderResponseDto result = controller.updateStatus(FIXED_UUID, dto).await().indefinitely();

            assertNotNull(result);
            verify(service).updateStatus(FIXED_UUID, dto);
        }

        @Test
        @DisplayName("should propagate WorkOrderLockedException")
        void shouldPropagateLockedException() {
            WorkOrderStatusUpdateRequestDto dto = new WorkOrderStatusUpdateRequestDto(WorkOrderStatus.DIAGNOSIS);
            when(service.updateStatus(FIXED_UUID, dto))
                    .thenReturn(Uni.createFrom().failure(new WorkOrderLockedException()));

            assertThrows(WorkOrderLockedException.class,
                    () -> controller.updateStatus(FIXED_UUID, dto).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("POST /v1/work-orders/{id}/estimate — createEstimate")
    class CreateEstimate {

        @Test
        @DisplayName("should return HTTP 201 with the created estimate")
        void shouldReturn201WithCreatedEstimate() {
            EstimateRequestDto dto = new EstimateRequestDto(
                    List.of(new EstimateItemRequestDto(UUID.randomUUID(), 1, BigDecimal.TEN)));
            EstimateResponseDto estimateResponse = new EstimateResponseDto(
                    UUID.randomUUID(), FIXED_UUID, EstimateStatus.PENDING, BigDecimal.TEN, BigDecimal.ZERO,
                    BigDecimal.TEN, null, null, List.of());

            when(service.createEstimate(FIXED_UUID, dto)).thenReturn(Uni.createFrom().item(estimateResponse));

            Response result = controller.createEstimate(FIXED_UUID, dto).await().indefinitely();

            assertEquals(201, result.getStatus());
            assertEquals(estimateResponse, result.getEntity());
            verify(service).createEstimate(FIXED_UUID, dto);
        }
    }

    @Nested
    @DisplayName("PATCH /v1/work-orders/{id}/estimate/{estimateId}/approve — approveEstimate")
    class ApproveEstimate {

        @Test
        @DisplayName("should return the approved estimate")
        void shouldReturnApprovedEstimate() {
            UUID estimateId = UUID.randomUUID();
            EstimateResponseDto estimateResponse = new EstimateResponseDto(
                    estimateId, FIXED_UUID, EstimateStatus.APPROVED, BigDecimal.TEN, BigDecimal.ZERO,
                    BigDecimal.TEN, null, null, List.of());

            when(service.approveEstimate(FIXED_UUID, estimateId)).thenReturn(Uni.createFrom().item(estimateResponse));

            EstimateResponseDto result = controller.approveEstimate(FIXED_UUID, estimateId).await().indefinitely();

            assertEquals(EstimateStatus.APPROVED, result.status());
            verify(service).approveEstimate(FIXED_UUID, estimateId);
        }

        @Test
        @DisplayName("should propagate EstimateNotApprovedException-like business failures")
        void shouldPropagateBusinessException() {
            UUID estimateId = UUID.randomUUID();
            when(service.approveEstimate(FIXED_UUID, estimateId))
                    .thenReturn(Uni.createFrom().failure(new EstimateNotApprovedException()));

            assertThrows(EstimateNotApprovedException.class,
                    () -> controller.approveEstimate(FIXED_UUID, estimateId).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("PATCH /v1/work-orders/{id}/estimate/{estimateId}/reject — rejectEstimate")
    class RejectEstimate {

        @Test
        @DisplayName("should return the rejected estimate")
        void shouldReturnRejectedEstimate() {
            UUID estimateId = UUID.randomUUID();
            EstimateResponseDto estimateResponse = new EstimateResponseDto(
                    estimateId, FIXED_UUID, EstimateStatus.REJECTED, BigDecimal.TEN, BigDecimal.ZERO,
                    BigDecimal.TEN, null, null, List.of());

            when(service.rejectEstimate(FIXED_UUID, estimateId)).thenReturn(Uni.createFrom().item(estimateResponse));

            EstimateResponseDto result = controller.rejectEstimate(FIXED_UUID, estimateId).await().indefinitely();

            assertEquals(EstimateStatus.REJECTED, result.status());
            verify(service).rejectEstimate(FIXED_UUID, estimateId);
        }
    }

    @Nested
    @DisplayName("POST /v1/work-orders/{id}/services — addService")
    class AddService {

        @Test
        @DisplayName("should return HTTP 201 with the created service line")
        void shouldReturn201WithCreatedService() {
            WorkOrderServiceRequestDto dto = new WorkOrderServiceRequestDto("Troca de oleo", BigDecimal.TEN);
            WorkOrderServiceResponseDto serviceResponse = new WorkOrderServiceResponseDto(
                    UUID.randomUUID(), FIXED_UUID, "Troca de oleo", BigDecimal.TEN, null);

            when(service.addService(FIXED_UUID, dto)).thenReturn(Uni.createFrom().item(serviceResponse));

            Response result = controller.addService(FIXED_UUID, dto).await().indefinitely();

            assertEquals(201, result.getStatus());
            assertEquals(serviceResponse, result.getEntity());
            verify(service).addService(FIXED_UUID, dto);
        }
    }

    @Nested
    @DisplayName("PATCH /v1/work-orders/{id}/close — close")
    class Close {

        @Test
        @DisplayName("should return the closed work order")
        void shouldReturnClosedWorkOrder() {
            WorkOrderCloseRequestDto dto = new WorkOrderCloseRequestDto(BigDecimal.valueOf(300));
            when(service.close(FIXED_UUID, dto)).thenReturn(Uni.createFrom().item(response));

            WorkOrderResponseDto result = controller.close(FIXED_UUID, dto).await().indefinitely();

            assertNotNull(result);
            verify(service).close(FIXED_UUID, dto);
        }
    }
}
