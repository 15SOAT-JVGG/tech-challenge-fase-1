package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.fase1.part.adapter.out.persistence.entity.PartJpaEntity;
import br.com.fiap.postech.soat16.fase1.part.domain.model.Part;
import br.com.fiap.postech.soat16.fase1.part.domain.model.enums.PartType;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.entity.EstimateItemJpaEntity;
import br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.entity.EstimateJpaEntity;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.Estimate;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.EstimateItem;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrder;
import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.EstimateStatus;

@DisplayName("EstimatePersistenceMapper — Unit Tests")
class EstimatePersistenceMapperTest {

    private static final UUID WORK_ORDER_ID = UUID.randomUUID();
    private static final UUID PART_ID = UUID.randomUUID();
    private static final LocalDateTime NOW = LocalDateTime.now();

    private Mutiny.Session session;

    @BeforeEach
    void setUp() {
        session = mock(Mutiny.Session.class);
        when(session.getReference(eq(PartJpaEntity.class), any())).thenAnswer(invocation -> {
            var reference = new PartJpaEntity();
            reference.setId(invocation.getArgument(1));
            return reference;
        });
    }

    private Estimate domain() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(WORK_ORDER_ID);

        Part part = Part.restore(
                PART_ID, "Filtro", "desc", new BigDecimal("50.00"), 10, "UN", 2,
                PartType.PART, NOW, NOW);

        EstimateItem item = EstimateItem.create(part, 2, new BigDecimal("50.00"));
        return Estimate.create(workOrder, List.of(item), List.of());
    }

    @Test
    @DisplayName("round trip preserves the estimate, its items and the part reference")
    void roundTripPreservesItems() {
        EstimateJpaEntity entity = EstimatePersistenceMapper.toJpaEntity(domain(), session);

        Estimate result = EstimatePersistenceMapper.toDomain(entity);

        assertEquals(WORK_ORDER_ID, result.getWorkOrder().getId());
        assertEquals(EstimateStatus.PENDING, result.getStatus());
        assertEquals(new BigDecimal("100.00"), result.getTotalAmount());
        assertEquals(1, result.getItems().size());

        EstimateItem item = result.getItems().getFirst();
        assertEquals(PART_ID, item.getPart().getId());
        assertEquals(2, item.getQuantity());
        assertEquals(new BigDecimal("100.00"), item.getTotalPrice());
        assertSame(result, item.getEstimate());
    }

    @Test
    @DisplayName("items keep the back reference to the owning estimate entity")
    void itemsKeepBackReference() {
        EstimateJpaEntity entity = EstimatePersistenceMapper.toJpaEntity(domain(), session);

        EstimateItemJpaEntity item = entity.getItems().getFirst();

        assertSame(entity, item.getEstimate());
        assertEquals(PART_ID, item.getPart().getId());
    }

    @Test
    @DisplayName("copyState carries the approval decision but leaves the items untouched")
    void copyStateCarriesApproval() {
        EstimateJpaEntity entity = EstimatePersistenceMapper.toJpaEntity(domain(), session);
        Estimate approved = domain();
        approved.approve(NOW);

        EstimatePersistenceMapper.copyState(approved, entity);

        assertEquals(EstimateStatus.APPROVED, entity.getStatus());
        assertEquals(NOW, entity.getApprovedAt());
        assertEquals(1, entity.getItems().size());
    }

    @Test
    @DisplayName("copyGeneratedState returns generated ids for the estimate and its items")
    void copyGeneratedStateFeedsBackIds() {
        Estimate estimate = domain();
        EstimateJpaEntity entity = EstimatePersistenceMapper.toJpaEntity(estimate, session);
        UUID estimateId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        entity.setId(estimateId);
        entity.getItems().getFirst().setId(itemId);

        EstimatePersistenceMapper.copyGeneratedState(entity, estimate);

        assertEquals(estimateId, estimate.getId());
        assertEquals(itemId, estimate.getItems().getFirst().getId());
    }

    @Test
    @DisplayName("null inputs map to null")
    void nullInputsMapToNull() {
        assertNull(EstimatePersistenceMapper.toDomain(null));
        assertNull(EstimatePersistenceMapper.toJpaEntity(null, session));
    }
}
