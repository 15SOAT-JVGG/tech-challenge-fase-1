package br.com.fiap.postech.soat16.fase1.workorder.application.port.out;

import br.com.fiap.postech.soat16.fase1.workorder.domain.model.WorkOrderTrackingToken;

/**
 * Assina e confere os links de acompanhamento. A assinatura só prova que o link saiu da oficina;
 * decidir se o prazo de acompanhamento já passou é do domínio, com a data de emissão que a
 * conferência devolve.
 */
public interface WorkOrderTrackingTokenSignaturePort {

    String sign(WorkOrderTrackingToken token);

    /**
     * @return o acompanhamento que a oficina emitiu, com o prazo que ele carrega
     * @throws br.com.fiap.postech.soat16.fase1.workorder.domain.exception.InvalidWorkOrderTrackingTokenException
     *         quando o link foi adulterado ou não é um token de acompanhamento
     */
    WorkOrderTrackingToken read(String signedToken);
}
