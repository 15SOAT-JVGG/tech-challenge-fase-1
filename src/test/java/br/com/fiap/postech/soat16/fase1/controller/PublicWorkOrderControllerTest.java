package br.com.fiap.postech.soat16.fase1.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.postech.soat16.fase1.dto.response.EstimateResponseDto;
import br.com.fiap.postech.soat16.fase1.dto.response.WorkOrderResponseDto;
import br.com.fiap.postech.soat16.fase1.exception.EstimateAlreadyDecidedException;
import br.com.fiap.postech.soat16.fase1.exception.EstimateNotFoundException;
import br.com.fiap.postech.soat16.fase1.exception.WorkOrderNotFoundException;
import br.com.fiap.postech.soat16.fase1.model.EstimateStatus;
import br.com.fiap.postech.soat16.fase1.model.WorkOrderPriority;
import br.com.fiap.postech.soat16.fase1.model.WorkOrderStatus;
import br.com.fiap.postech.soat16.fase1.service.WorkOrderService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicWorkOrderController — Unit Tests")
class PublicWorkOrderControllerTest {

    @Mock
    private WorkOrderService service;

    private PublicWorkOrderController controller;

    private static final UUID WORK_ORDER_ID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    private static final UUID ESTIMATE_ID = UUID.fromString("c3b79cde-2872-4053-9622-37605bf124a3");

    private WorkOrderResponseDto workOrderResponse;
    private EstimateResponseDto estimateResponse;

    @BeforeEach
    void setUp() {
        controller = new PublicWorkOrderController(service);
        workOrderResponse = new WorkOrderResponseDto(WORK_ORDER_ID, UUID.randomUUID(), UUID.randomUUID(), "desc",
            WorkOrderPriority.MEDIUM, WorkOrderStatus.WAITING_APPROVAL, null, null, null, null);
        estimateResponse = new EstimateResponseDto(ESTIMATE_ID, WORK_ORDER_ID, EstimateStatus.PENDING,
            new BigDecimal("50.00"), new BigDecimal("0.00"), new BigDecimal("50.00"), null, null, null);
    }

    @Nested
    @DisplayName("GET /v1/public/work-orders/{id} — track")
    class Track {

        @Test
        @DisplayName("should return work order without requiring authentication")
        void shouldReturnWorkOrder() {
            when(service.findById(WORK_ORDER_ID)).thenReturn(Uni.createFrom().item(workOrderResponse));

            WorkOrderResponseDto result = controller.track(WORK_ORDER_ID).await().indefinitely();

            assertEquals(WorkOrderStatus.WAITING_APPROVAL, result.status());
            verify(service).findById(WORK_ORDER_ID);
        }

        @Test
        @DisplayName("should propagate WorkOrderNotFoundException")
        void shouldPropagateNotFound() {
            when(service.findById(WORK_ORDER_ID))
                .thenReturn(Uni.createFrom().failure(new WorkOrderNotFoundException()));

            assertThrows(WorkOrderNotFoundException.class,
                () -> controller.track(WORK_ORDER_ID).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("PATCH /v1/public/work-orders/{id}/estimate/{estimateId}/approve — approveEstimate")
    class ApproveEstimate {

        @Test
        @DisplayName("should approve estimate via public channel")
        void shouldApprove() {
            EstimateResponseDto approved = new EstimateResponseDto(ESTIMATE_ID, WORK_ORDER_ID, EstimateStatus.APPROVED,
                new BigDecimal("50.00"), new BigDecimal("0.00"), new BigDecimal("50.00"), null, null, null);
            when(service.approveEstimate(WORK_ORDER_ID, ESTIMATE_ID)).thenReturn(Uni.createFrom().item(approved));

            EstimateResponseDto result = controller.approveEstimate(WORK_ORDER_ID, ESTIMATE_ID).await().indefinitely();

            assertEquals(EstimateStatus.APPROVED, result.status());
            verify(service).approveEstimate(WORK_ORDER_ID, ESTIMATE_ID);
        }

        @Test
        @DisplayName("should propagate EstimateAlreadyDecidedException")
        void shouldPropagateAlreadyDecided() {
            when(service.approveEstimate(WORK_ORDER_ID, ESTIMATE_ID))
                .thenReturn(Uni.createFrom().failure(new EstimateAlreadyDecidedException()));

            assertThrows(EstimateAlreadyDecidedException.class,
                () -> controller.approveEstimate(WORK_ORDER_ID, ESTIMATE_ID).await().indefinitely());
        }

        @Test
        @DisplayName("should propagate EstimateNotFoundException")
        void shouldPropagateNotFound() {
            when(service.approveEstimate(WORK_ORDER_ID, ESTIMATE_ID))
                .thenReturn(Uni.createFrom().failure(new EstimateNotFoundException()));

            assertThrows(EstimateNotFoundException.class,
                () -> controller.approveEstimate(WORK_ORDER_ID, ESTIMATE_ID).await().indefinitely());
        }
    }

    @Nested
    @DisplayName("PATCH /v1/public/work-orders/{id}/estimate/{estimateId}/reject — rejectEstimate")
    class RejectEstimate {

        @Test
        @DisplayName("should reject estimate via public channel")
        void shouldReject() {
            EstimateResponseDto rejected = new EstimateResponseDto(ESTIMATE_ID, WORK_ORDER_ID, EstimateStatus.REJECTED,
                new BigDecimal("50.00"), new BigDecimal("0.00"), new BigDecimal("50.00"), null, null, null);
            when(service.rejectEstimate(WORK_ORDER_ID, ESTIMATE_ID)).thenReturn(Uni.createFrom().item(rejected));

            EstimateResponseDto result = controller.rejectEstimate(WORK_ORDER_ID, ESTIMATE_ID).await().indefinitely();

            assertEquals(EstimateStatus.REJECTED, result.status());
            verify(service).rejectEstimate(WORK_ORDER_ID, ESTIMATE_ID);
        }

        @Test
        @DisplayName("should propagate EstimateAlreadyDecidedException")
        void shouldPropagateAlreadyDecided() {
            when(service.rejectEstimate(WORK_ORDER_ID, ESTIMATE_ID))
                .thenReturn(Uni.createFrom().failure(new EstimateAlreadyDecidedException()));

            assertThrows(EstimateAlreadyDecidedException.class,
                () -> controller.rejectEstimate(WORK_ORDER_ID, ESTIMATE_ID).await().indefinitely());
        }
    }
}
