# Fluxo da Ordem de Serviço (Work Order)

Este documento explica o ciclo de vida de uma **ordem de serviço** (work order) na oficina mecânica:
estados possíveis, regras de negócio e o passo a passo dos endpoints, do abertura até a entrega do
veículo. Para detalhes de autenticação, portas e como rodar o projeto, veja o [README.md](README.md).

---

## Visão geral das entidades

| Entidade | O que representa |
|---|---|
| `WorkOrder` | A ordem de serviço em si: cliente, veículo, mecânico responsável, descrição, prioridade, status e valores. |
| `Estimate` | Um orçamento de peças vinculado à ordem de serviço. Pode ser aprovado. |
| `EstimateItem` | Um item do orçamento: uma peça do catálogo (`Part`), quantidade e preço unitário. |
| `WorkOrderService` | Uma linha de mão de obra executada na ordem (ex.: "Troca de óleo"), com descrição, preço e item do catálogo de serviços de origem. |
| `WorkOrderHistory` | Registro interno de toda mudança de status da ordem (não exposto via API). |

---

## Máquina de estados (`status`)

```
RECEIVED ──► DIAGNOSIS ──► WAITING_APPROVAL ──► IN_PROGRESS ──► COMPLETED ──► DELIVERED
                                  │
                                  └── recusa ──► COMPLETED + cancelledAt
```

- **RECEIVED** é o status inicial — definido automaticamente na criação, junto com `openedAt`.
- **COMPLETED** é alcançado pelo endpoint dedicado `PATCH /{id}/close` ou pela recusa do orçamento.
- **DELIVERED** só pode ser definido a partir de `COMPLETED`, via `PATCH /{id}/status`.
- Não existe cancelamento manual nem status `CANCELLED`: a recusa conclui a OS e preenche
  `cancelledAt`.
- **DELIVERED** e uma OS concluída por recusa são terminais: qualquer tentativa de alterá-las
  retorna erro (`WorkOrderLockedException`, HTTP 422).
- Não é permitido **pular etapas** via `PATCH /status` (ex.: ir de `RECEIVED` direto para
  `IN_PROGRESS`).
- Toda mudança de status (pelo `PATCH /status`, pelo `/close`, ou implicitamente ao aprovar um
  orçamento ou registrar uma recusa) gera um registro em `WorkOrderHistory` com status anterior,
  novo status e data/hora.

### Prioridade (`priority`)

`LOW`, `MEDIUM` (padrão quando não informada), `HIGH`, `URGENT`. A prioridade é um atributo de
triagem da ordem; ela não influencia a ordenação da listagem operacional.

### Fila operacional (`GET /v1/work-orders`)

A listagem administrativa é a fila de trabalho da oficina, e não um espelho de tudo que já passou
por ela:

- só entram as OS ainda em atendimento — `COMPLETED` e `DELIVERED` ficam de fora, inclusive as
  concluídas por recusa do orçamento;
- os grupos aparecem na ordem `IN_PROGRESS`, `WAITING_APPROVAL`, `DIAGNOSIS` e `RECEIVED`;
- dentro do mesmo status, a OS aberta há mais tempo vem primeiro;
- a contagem e a paginação (`page`, `size`) consideram exatamente esse conjunto filtrado.

---

## Regras de orçamento (Estimate)

- O orçamento inicial nasce junto da OS quando a abertura traz `services` ou `parts`, com status
  `PENDING` e sem `sentAt` — ele só é enviado ao cliente mais adiante no ciclo.
- `EstimateItem.totalPrice = quantity * unitPrice`. Se `unitPrice` não for informado na requisição,
  o valor unitário da peça no catálogo (`Part.unitPrice`) é usado. Na abertura o preço é sempre o do
  catálogo, copiado como snapshot.
- `Estimate.totalAmount` é a soma do `totalPrice` de todos os itens.
- Ao **aprovar** um orçamento (`PATCH /estimate/{estimateId}/approve`):
  - `WorkOrder.estimatedValue` recebe o `totalAmount` do orçamento aprovado.
  - Se a ordem estiver em `WAITING_APPROVAL`, ela avança automaticamente para `IN_PROGRESS` (gerando
    histórico). Se já estiver em outro status, apenas o `estimatedValue` é atualizado.
  - Um orçamento já aprovado ou rejeitado não pode ser aprovado novamente (`EstimateAlreadyDecidedException`, HTTP 409).
- Ao **recusar** um orçamento pendente em `WAITING_APPROVAL`, a OS avança para `COMPLETED`, recebe
  `closedAt` e `cancelledAt` e fica bloqueada para novas alterações.
- Uma ordem **não pode iniciar a execução** (`IN_PROGRESS`) **nem ser fechada** (`/close`) sem um
  orçamento aprovado (`EstimateNotApprovedException`, HTTP 422).

---

## Endpoints

| Método | Caminho | Descrição |
|---|---|---|
| `POST` | `/v1/work-orders` | Abre uma nova ordem de serviço e, junto dela, o orçamento pendente da solicitação inicial. |
| `GET` | `/v1/work-orders/{id}` | Busca uma ordem por id. |
| `GET` | `/v1/work-orders` | Lista a fila operacional: apenas OS em atendimento, paginadas e ordenadas por estágio de trabalho. |
| `PATCH` | `/v1/work-orders/{id}/status` | Avança o status da ordem (exceto para `COMPLETED`). |
| `POST` | `/v1/work-orders/{id}/estimate` | Cria um orçamento com itens do catálogo de peças. |
| `PATCH` | `/v1/work-orders/{id}/estimate/{estimateId}/approve` | Aprova um orçamento pendente. |
| `PATCH` | `/v1/work-orders/{id}/estimate/{estimateId}/reject` | Recusa o orçamento e conclui a OS com `cancelledAt`. |
| `POST` | `/v1/work-orders/{id}/services` | Registra uma linha de mão de obra executada. |
| `PATCH` | `/v1/work-orders/{id}/close` | Finaliza a ordem (`IN_PROGRESS` → `COMPLETED`). |

---

## Passo a passo (exemplo completo)

> Os exemplos usam `curl`. Troque `$TOKEN` pelo token JWT (veja [docs/FASE-1.md](docs/FASE-1.md#autenticação-jwt)) se o ambiente exigir autenticação, e os UUIDs pelos valores reais retornados em cada chamada.

### 1. Abrir a ordem de serviço

```shell
curl -s -X POST http://localhost:8080/v1/work-orders \
  -H "Content-Type: application/json" \
  -d '{
        "customerId": "<customer-uuid>",
        "vehicleId": "<vehicle-uuid>",
        "description": "Revisão dos 10.000km",
        "priority": "HIGH",
        "assignedWorkerId": "<worker-uuid>",
        "services": [{"serviceItemId": "<service-item-uuid>"}],
        "parts": [{"partId": "<part-uuid>", "quantity": 2}]
      }'
```

A ordem é criada com `status = RECEIVED`, `openedAt` preenchido automaticamente. `priority` é opcional —
se omitido, assume `MEDIUM`. `assignedWorkerId` — identifica o mecânico responsável pela OS.

`services` e `parts` formam a **solicitação inicial** e são opcionais. Quando qualquer um deles vem
preenchido, a mesma transação cria a OS e o orçamento pendente correspondente: os serviços viram
linhas de mão de obra com o `basePrice` do catálogo e as peças viram itens do orçamento com o
`unitPrice` do catálogo. Como os preços são copiados na abertura, uma atualização posterior do
catálogo não altera o orçamento já emitido. Uma referência inválida (cliente, veículo, mecânico,
item de serviço ou peça) faz a requisição inteira falhar sem deixar dados parciais.

A resposta `201` traz a ordem aberta e o orçamento inicial, além do cabeçalho `Location` apontando
para a nova OS:

```json
{
  "workOrder": {"workOrderId": "...", "status": "RECEIVED", "estimatedValue": 270.00},
  "estimate": {"estimateId": "...", "status": "PENDING", "partsAmount": 150.00,
               "laborAmount": 120.00, "totalAmount": 270.00, "items": []}
}
```

`estimate` vem `null` quando a abertura não traz solicitação inicial.

### 2. Avançar para diagnóstico

```shell
curl -s -X PATCH http://localhost:8080/v1/work-orders/<work-order-id>/status \
  -H "Content-Type: application/json" \
  -d '{"status": "DIAGNOSIS"}'
```

### 3. Criar o orçamento

```shell
curl -s -X POST http://localhost:8080/v1/work-orders/<work-order-id>/estimate \
  -H "Content-Type: application/json" \
  -d '{
        "items": [
          {"partId": "<part-uuid-1>", "quantity": 1},
          {"partId": "<part-uuid-2>", "quantity": 4, "unitPrice": 25.50}
        ]
      }'
```

A resposta traz o `estimateId`, o `totalAmount` calculado e cada item com seu `totalPrice`.
A criação do orçamento move a ordem automaticamente de `DIAGNOSIS` para `WAITING_APPROVAL`.

### 4. Aprovar o orçamento

```shell
curl -s -X PATCH http://localhost:8080/v1/work-orders/<work-order-id>/estimate/<estimate-id>/approve
```

A ordem (que estava em `WAITING_APPROVAL`) passa automaticamente para `IN_PROGRESS`, e
`estimatedValue` recebe o `totalAmount` do orçamento.

### 5. Registrar a mão de obra executada

```shell
curl -s -X POST http://localhost:8080/v1/work-orders/<work-order-id>/services \
  -H "Content-Type: application/json" \
  -d '{"description": "Troca de óleo e filtros", "price": 120.00, "serviceItemId": "<service-item-uuid>"}'
```

Pode ser chamado várias vezes — cada chamada cria uma linha de `WorkOrderService` independente.
`serviceItemId` — vincula a linha ao item do catálogo de serviços que a originou.

### 6. Fechar a ordem

```shell
curl -s -X PATCH http://localhost:8080/v1/work-orders/<work-order-id>/close \
  -H "Content-Type: application/json" \
  -d '{"finalValue": 410.50}'
```

Exige status `IN_PROGRESS` e orçamento aprovado. `finalValue` é opcional — se omitido, usa o
`estimatedValue` da ordem. Pode ser usado para registrar um valor final diferente do orçamento
aprovado (ex.: serviço extra identificado durante a execução ou desconto concedido); quando
informado, deve ser maior que zero. Define `closedAt` e move o status para `COMPLETED`.

### 7. Entregar o veículo

```shell
curl -s -X PATCH http://localhost:8080/v1/work-orders/<work-order-id>/status \
  -H "Content-Type: application/json" -d '{"status": "DELIVERED"}'
```

A partir daqui a ordem está **bloqueada**: nenhuma alteração de status, orçamento ou serviço é
mais aceita.

### Recusar o orçamento

O cliente pode recusar um orçamento pendente:

```shell
curl -s -X PATCH \
  http://localhost:8080/v1/public/work-orders/<work-order-id>/estimate/<estimate-id>/reject
```

A OS passa para `COMPLETED`, recebe `closedAt` e `cancelledAt` e não pode ser entregue nem alterada.
Não existe endpoint de cancelamento manual.

---

## Erros mais comuns

| HTTP | Código | Quando acontece |
|---|---|---|
| 404 | `WORK_ORDER_NOT_FOUND` | Id de ordem inexistente. |
| 404 | `ESTIMATE_PART_NOT_FOUND` | `partId` informado no orçamento não existe no catálogo. |
| 404 | `ESTIMATE_NOT_FOUND` | `estimateId` não existe ou não pertence à ordem informada. |
| 409 | `ESTIMATE_ALREADY_DECIDED` | Tentativa de aprovar um orçamento já aprovado/rejeitado. |
| 422 | `WORK_ORDER_LOCKED` | Ordem já `DELIVERED` ou concluída por recusa do orçamento. |
| 422 | `INVALID_STATUS_TRANSITION` | Transição de status inválida (pular etapa, ir direto para `COMPLETED`, etc.). |
| 422 | `ESTIMATE_NOT_APPROVED` | Tentativa de iniciar (`IN_PROGRESS`) ou fechar (`/close`) sem orçamento aprovado. |

---

## Cobertura de testes

O fluxo é validado em duas camadas, ambas no pacote `br.com.fiap.postech.soat16.fase1`:

- **Unitário** — `workorder.application.WorkOrderServiceTest`: regras de negócio isoladas, portas mockadas.
  Cobre também transições artificiais (ex.: travas combinadas) difíceis de reproduzir via API.
- **Integração** — `workorder.adapter.in.rest.controller.WorkOrderControllerIT`: mesmo fluxo no endpoint HTTP real
  contra um PostgreSQL via Testcontainers (`./mvnw test -Pitest`, requer Docker) — valida
  serialização JSON, mapeamento JPA/Hibernate Reactive e Bean Validation de ponta a ponta.

A tabela liga cada regra já descrita neste documento ao teste que a exercita (nomes abreviados
como `Classe.método`; todas vivem dentro de uma classe `@Nested` com o mesmo nome do endpoint):

| Regra de negócio | Integração (`WorkOrderControllerIT`) | Unitário (`WorkOrderServiceTest`) |
|---|---|---|
| Ordem nasce em `RECEIVED`, `openedAt` automático | `FullLifecycle.shouldCompleteFullLifecycle`, `Create.shouldCreateWorkOrder` | `Create.shouldPersistWorkOrder...` |
| Abertura devolve a identificação da OS no corpo e no `Location` | `Create.shouldReturnCreatedWorkOrderIdentification` | `WorkOrderControllerTest.Create.shouldReturn201WhenCreateSucceeds` |
| Solicitação inicial abre OS e orçamento pendente na mesma transação | `Create.shouldOpenWorkOrderWithInitialPendingEstimate` | `Create.shouldCreatePendingEstimateFromInitialRequest` |
| Preços do orçamento são snapshot do catálogo | `Create.shouldKeepEstimatePricesAfterCatalogUpdate` | `Create.shouldSnapshotServicePrice` |
| Item de serviço ou peça inválido na abertura → 404 sem dados parciais | `Create.shouldReturn404WhenServiceItemNotFound` / `...RequestedPartNotFound` | `Create.shouldThrowWhenRequestedServiceItemMissing` / `...RequestedPartMissing` |
| Cliente/veículo inexistente ao criar → 404 | `Create.shouldReturn404WhenCustomerNotFound` / `...VehicleNotFound` | `Create.shouldThrow...NotFoundException` |
| Não é permitido pular etapas (ex.: `RECEIVED` → `IN_PROGRESS` direto) | `UpdateStatus.shouldRejectSkippingCanonicalStages` | `UpdateStatus.shouldRejectSkippingStages` |
| `COMPLETED` só via `/close`, nunca pelo `/status` | `UpdateStatus.shouldRejectCompletedViaGenericEndpoint` | `UpdateStatus.shouldRejectJumpingDirectlyToCompleted` |
| A recusa conclui a OS e preenche `cancelledAt` | `EstimateDecisionAndStock.shouldRejectEstimateAndCompleteWorkOrder` | `RejectEstimate.shouldRejectAndCompleteWorkOrder` |
| `DELIVERED` e conclusão por recusa bloqueiam alterações (`WORK_ORDER_LOCKED`) | `FullLifecycle.shouldLockWorkOrderAfterDelivered`, `EstimateDecisionAndStock.shouldLockWorkOrderAfterEstimateRejection` | `WorkOrderTest.rejectsDeliveryAfterEstimateRejection` |
| Orçamento aprovado grava `estimatedValue` na ordem | `ApproveEstimate.shouldApproveAndAdvanceStatus` | `ApproveEstimate.shouldApproveEstimateSetEstimatedValue...` |
| Aprovar orçamento em `WAITING_APPROVAL` avança a ordem para `IN_PROGRESS` automaticamente | `ApproveEstimate.shouldApproveAndAdvanceStatus` | `ApproveEstimate.shouldApproveAndAdvanceStatus` |
| Orçamento já decidido não pode ser aprovado de novo → 409 | `ApproveEstimate.shouldReturn409WhenAlreadyDecided` | `ApproveEstimate.shouldThrowEstimateAlreadyDecidedException` |
| Ordem não pode ir para `IN_PROGRESS` sem orçamento aprovado | `UpdateStatus.shouldRejectApprovingWithoutApprovedEstimate` | `UpdateStatus.shouldThrowWhenStartingWithoutApprovedEstimate` |
| `/close` exige orçamento aprovado e status `IN_PROGRESS` | `Close.shouldReturn422WhenNotInProgress` | `Close.shouldThrow...WhenNotInProgress`, `...WhenThereIsNoApprovedEstimate` |
| `finalValue` opcional no fechamento — usa `estimatedValue` se omitido | `Close.shouldCloseUsingEstimatedValueWhenOmitted` | `Close.shouldCompleteUsingTheApprovedEstimateValueWhenFinalValueIsOmitted` |
| `finalValue` pode divergir do orçamento (serviço extra/desconto) | `Close.shouldCloseUsingProvidedFinalValue` | `Close.shouldUseTheProvidedFinalValueWhenPresent`, `...shouldRecordHistoryTransitionToCompletedWhenFinalValueDiffersFromTheEstimate` |
| Peça inexistente no orçamento → 404 | `CreateEstimate.shouldReturn404WhenPartNotFound` | `CreateEstimate.shouldThrowEstimatePartNotFoundException` |
| Itens de orçamento vazios são rejeitados → 400 | `CreateEstimate.shouldReturn400WhenItemsEmpty` | — (Bean Validation, não exercida no nível de serviço) |
| Mão de obra pode ser registrada múltiplas vezes na mesma ordem | `AddService.shouldAddService` | `AddService.shouldPersistALaborServiceLine` |
| Fila operacional agrupa por estágio e ordena pela OS mais antiga | `OperationalQueue.shouldGroupByWorkStage`, `...shouldPlaceTheOldestFirstWithinTheSameStatus` | `WorkOrderStatusTest.OperationalQueue.ordersFromTheMostAdvancedStage` |
| Fila operacional exclui OS concluídas, entregues e recusadas | `OperationalQueue.shouldExcludeClosedWorkOrders` | `WorkOrderStatusTest.OperationalQueue.excludesClosedStatuses` |
| Contagem e paginação consideram só o conjunto filtrado | `OperationalQueue.shouldCountOnlyTheQueuedWorkOrders`, `...shouldPaginateTheFilteredSet`, `...shouldReturnAnEmptyPageBeyondTheLastOne` | `FindOperationalQueue.shouldReturnPaginatedResponse` |

Cenários de validação de payload (`@Valid`/Bean Validation, ex.: descrição em branco, preço/valor
zero) só existem na camada de integração — só ali a requisição passa pelo filtro JAX-RS real antes
de chegar ao serviço.
