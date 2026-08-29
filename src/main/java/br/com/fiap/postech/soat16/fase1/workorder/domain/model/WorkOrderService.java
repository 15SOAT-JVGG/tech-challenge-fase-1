package br.com.fiap.postech.soat16.fase1.workorder.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.servicecatalog.domain.model.ServiceItem;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Serviço executado em uma {@link WorkOrder}, enquanto {@link EstimateItem} representa uma peça do
 * catálogo.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class WorkOrderService {

    @EqualsAndHashCode.Include
    private UUID id;

    private WorkOrder workOrder;

    private ServiceItem serviceItem;

    private String description;

    private BigDecimal price;

    private LocalDateTime performedAt;
}
