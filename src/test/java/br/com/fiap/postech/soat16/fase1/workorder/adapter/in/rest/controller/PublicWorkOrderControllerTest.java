package br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.controller;

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

import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.EstimateResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.in.rest.dto.response.WorkOrderResponseDto;
import br.com.fiap.postech.soat16.fase1.workorder.application.WorkOrderService;
import br.com.fiap.postech.soat16.fase1.workorder.application.result.EstimateResult;
import br.com.fiap.postech.soat16.fase1.workorder.application.result.WorkOrderResult;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.EstimateAlreadyDecidedException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.EstimateDecisionTokenAlreadyUsedException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.ExpiredEstimateDecisionTokenException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.InvalidEstimateDecisionTokenException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.exception.WorkOrderNotFoundException;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.EstimateStatus;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderPriority;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.WorkOrderStatus;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicWorkOrderController — Unit Tests")
class PublicWorkOrderControllerTest {

    @Mock
    private WorkOrderService service;

    private PublicWorkOrderController controller;

    private static final UUID WORK_ORDER_ID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    private static final UUID ESTIMATE_ID = UUID.fromString("c3b79cde-2872-4053-9622-37605bf124a3");
    private static final String DECISION_TOKEN = "signed-decision-token";

    private WorkOrderResult workOrderResponse;

    @BeforeEach
    void setUp() {
        controller = new PublicWorkOrderController(service);
        workOrderResponse = new WorkOrderResult(WORK_ORDER_ID, UUID.randomUUID(), UUID.randomUUID(), "desc",
            WorkOrderPriority.MEDIUM, WorkOrderStatus.WAITING_APPROVAL, null, null, null, null, null, null);
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
    @DisplayName("POST /v1/public/work-orders/estimate-decisions/{token} — decideEstimate")
    class DecideEstimate {

        @Test
        @DisplayName("deve registrar a aprovação enviada pelo link do cliente")
        void shouldRegisterApproval() {
            when(service.decideEstimate(DECISION_TOKEN))
                .thenReturn(Uni.createFrom().item(decision(EstimateStatus.APPROVED)));

            EstimateResponseDto result = controller.decideEstimate(DECISION_TOKEN).await().indefinitely();

            assertEquals(EstimateStatus.APPROVED, result.status());
            verify(service).decideEstimate(DECISION_TOKEN);
        }

        @Test
        @DisplayName("deve registrar a recusa enviada pelo link do cliente")
        void shouldRegisterRejection() {
            when(service.decideEstimate(DECISION_TOKEN))
                .thenReturn(Uni.createFrom().item(decision(EstimateStatus.REJECTED)));

            EstimateResponseDto result = controller.decideEstimate(DECISION_TOKEN).await().indefinitely();

            assertEquals(EstimateStatus.REJECTED, result.status());
        }

        @Test
        @DisplayName("deve propagar o link inválido")
        void shouldPropagateInvalidToken() {
            when(service.decideEstimate(DECISION_TOKEN))
                .thenReturn(Uni.createFrom().failure(new InvalidEstimateDecisionTokenException()));

            assertThrows(InvalidEstimateDecisionTokenException.class,
                () -> controller.decideEstimate(DECISION_TOKEN).await().indefinitely());
        }

        @Test
        @DisplayName("deve propagar o link expirado")
        void shouldPropagateExpiredToken() {
            when(service.decideEstimate(DECISION_TOKEN))
                .thenReturn(Uni.createFrom().failure(new ExpiredEstimateDecisionTokenException()));

            assertThrows(ExpiredEstimateDecisionTokenException.class,
                () -> controller.decideEstimate(DECISION_TOKEN).await().indefinitely());
        }

        @Test
        @DisplayName("deve propagar o link já utilizado")
        void shouldPropagateUsedToken() {
            when(service.decideEstimate(DECISION_TOKEN))
                .thenReturn(Uni.createFrom().failure(new EstimateDecisionTokenAlreadyUsedException()));

            assertThrows(EstimateDecisionTokenAlreadyUsedException.class,
                () -> controller.decideEstimate(DECISION_TOKEN).await().indefinitely());
        }

        @Test
        @DisplayName("deve propagar o orçamento já decidido")
        void shouldPropagateAlreadyDecided() {
            when(service.decideEstimate(DECISION_TOKEN))
                .thenReturn(Uni.createFrom().failure(new EstimateAlreadyDecidedException()));

            assertThrows(EstimateAlreadyDecidedException.class,
                () -> controller.decideEstimate(DECISION_TOKEN).await().indefinitely());
        }

        private EstimateResult decision(EstimateStatus status) {
            return new EstimateResult(ESTIMATE_ID, WORK_ORDER_ID, status, new BigDecimal("50.00"),
                new BigDecimal("0.00"), new BigDecimal("50.00"), null, null, null);
        }
    }
}
