# Acompanhamento público por link assinado, sem registro no banco

O acompanhamento do cliente deixa de ser um `GET` público por id e passa a exigir um
`WorkOrderTrackingToken` assinado, entregue por e-mail na abertura da OS e a cada mudança de status:
`GET /v1/public/work-orders/tracking/{token}`. A rota por id foi removida porque bastava conhecer o
identificador da ordem — que circula em logs, planilhas e no corpo de outras respostas — para ler o
atendimento de qualquer cliente.

O token de acompanhamento é distinto do de decisão porque as duas capacidades são diferentes.
Decidir um orçamento altera a OS e o estoque, então o link vale uma única vez e por sete dias.
Acompanhar não altera nada e é justamente o que o cliente vai querer fazer várias vezes, então o
link vale trinta dias e quantas consultas ele quiser. Reunir as duas coisas em um token só daria ao
acompanhamento o prazo da decisão, ou à decisão a longevidade do acompanhamento.

Por não haver nada a gastar, o acompanhamento não tem registro no banco: a emissão viaja assinada no
`iat` do JWT e é dela que o domínio deriva o prazo. Cada aviso reemite o link, de modo que o cliente
sempre tem trinta dias a partir da última novidade. Como no link de decisão, o `exp` do JWT é muito
mais longo que a janela, para que um link vencido seja lido e respondido como expirado (410) em vez
de morrer antes disso como se fosse forjado (400).

A resposta pública conta apenas `workOrderId`, `status`, `openedAt`, `closedAt` e `cancelledAt`. Um
e-mail é reencaminhado e um link é colado em conversas, então o que ele abre é o mínimo que responde
à pergunta "e o meu carro?" — valores, peças, descrição e mecânico continuam só no canal
administrativo.
