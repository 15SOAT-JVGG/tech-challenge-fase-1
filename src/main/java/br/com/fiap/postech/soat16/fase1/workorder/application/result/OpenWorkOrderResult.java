package br.com.fiap.postech.soat16.fase1.workorder.application.result;

/**
 * A ordem recém-aberta e o orçamento pendente gerado a partir da solicitação inicial. O orçamento é
 * nulo quando a abertura não trouxe peças nem serviços.
 */
public record OpenWorkOrderResult(
        WorkOrderResult workOrder,
        EstimateResult estimate
) { }
