package br.com.fiap.postech.soat16.fase1.workorder.application.port.out;

import java.time.LocalDateTime;

/**
 * O convite para acompanhar a ordem de serviço. A aplicação entrega o token assinado e o prazo;
 * montar a URL do link é do adaptador que envia a mensagem, dono do endereço público.
 */
public record WorkOrderTrackingInvitation(String trackingToken, LocalDateTime expiresAt) {
}
