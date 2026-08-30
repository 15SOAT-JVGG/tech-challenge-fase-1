package br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums;

/**
 * A decisão que o cliente toma sobre um orçamento pelo canal público. Cada uma tem o seu próprio
 * link de uso único no e-mail enviado quando a ordem entra em WAITING_APPROVAL.
 */
public enum EstimateDecision {

    APPROVE,
    REJECT
}
