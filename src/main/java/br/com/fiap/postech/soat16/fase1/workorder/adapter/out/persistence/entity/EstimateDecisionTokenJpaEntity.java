package br.com.fiap.postech.soat16.fase1.workorder.adapter.out.persistence.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.enums.EstimateDecision;

import lombok.Getter;
import lombok.Setter;

/**
 * O identificador é o mesmo que viaja assinado no link, e por isso é atribuído pelo domínio em vez
 * de gerado pelo banco.
 */
@Entity(name = "EstimateDecisionToken")
@Table(name = "estimate_decision_token", schema = "oficina_mecanica")
@Getter
@Setter
public class EstimateDecisionTokenJpaEntity {

    @Id
    @Column(name = "estimate_decision_token_id", nullable = false)
    private UUID id;

    @Column(name = "work_order_id", nullable = false)
    private UUID workOrderId;

    @Column(name = "estimate_id", nullable = false)
    private UUID estimateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstimateDecision decision;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;
}
