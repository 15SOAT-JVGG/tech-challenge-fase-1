package br.com.fiap.postech.soat16.fase1.workorder.application.port.out;

import java.time.LocalDateTime;

/**
 * O par de tokens assinados que dá ao cliente uma decisão sobre o orçamento. A aplicação entrega os
 * tokens; montar a URL de cada link é do adaptador que envia a mensagem, dono do endereço público.
 */
public record EstimateDecisionInvitation(String approveToken, String rejectToken,
        LocalDateTime expiresAt) {
}
