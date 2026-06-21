# Fluxo da Ordem de Serviço (Work Order)

Este documento explica o ciclo de vida de uma **ordem de serviço** (work order) na oficina mecânica:
estados possíveis, regras de negócio e o passo a passo dos endpoints, do abertura até a entrega do
veículo. Para detalhes de autenticação, portas e como rodar o projeto, veja o [README.md](README.md).

---

## Visão geral das entidades

| Entidade | O que representa |
|---|---|
| `WorkOrder` | A ordem de serviço em si: cliente, veículo, descrição, prioridade, status e valores. |
| `Estimate` | Um orçamento de peças vinculado à ordem de serviço. Pode ser aprovado. |
| `EstimateItem` | Um item do orçamento: uma peça do catálogo (`Part`), quantidade e preço unitário. |
| `WorkOrderService` | Uma linha de mão de obra executada na ordem (ex.: "Troca de óleo"), com descrição e preço. |
| `WorkOrderHistory` | Registro interno de toda mudança de status da ordem (não exposto via API). |

---

## Máquina de estados (`status`)

```
RECEIVED ──► DIAGNOSIS ──► WAITING_APPROVAL ──► APPROVED ──► IN_PROGRESS ──► COMPLETED ──► DELIVERED
  │            │               │               │              │
  └────────────┴───────────────┴───────────────┴──────────────┴──────────────► CANCELLED
```

- **RECEIVED** é o status inicial — definido automaticamente na criação, junto com `openedAt`.
- **COMPLETED** só é alcançado pelo endpoint dedicado `PATCH /{id}/close` (não pelo `PATCH /{id}/status`).
- **DELIVERED** só pode ser definido a partir de `COMPLETED`, via `PATCH /{id}/status`.
- **CANCELLED** pode ser definido a partir de qualquer status não terminal (`RECEIVED`, `DIAGNOSIS`,
  `WAITING_APPROVAL`, `APPROVED`, `IN_PROGRESS` ou `COMPLETED`).
- **DELIVERED** e **CANCELLED** são terminais: qualquer tentativa de alterar a ordem depois disso
  retorna erro (`WorkOrderLockedException`, HTTP 422).
- Não é permitido **pular etapas** via `PATCH /status` (ex.: ir de `RECEIVED` direto para `APPROVED`).
- Toda mudança de status (pelo `PATCH /status`, pelo `/close`, ou implicitamente ao aprovar um
  orçamento) gera um registro em `WorkOrderHistory` com status anterior, novo status e data/hora.

### Prioridade (`priority`)

`LOW`, `MEDIUM` (padrão quando não informada), `HIGH`, `URGENT`. A listagem
(`GET /v1/work-orders`) é sempre ordenada por prioridade — `URGENT` primeiro, `LOW` por último — e,
dentro da mesma prioridade, pelas mais recentes primeiro.

---

## Regras de orçamento (Estimate)

- `EstimateItem.totalPrice = quantity * unitPrice`. Se `unitPrice` não for informado na requisição,
  o valor unitário da peça no catálogo (`Part.unitPrice`) é usado.
- `Estimate.totalAmount` é a soma do `totalPrice` de todos os itens.
- Ao **aprovar** um orçamento (`PATCH /estimate/{estimateId}/approve`):
  - `WorkOrder.estimatedValue` recebe o `totalAmount` do orçamento aprovado.
  - Se a ordem estiver em `WAITING_APPROVAL`, ela avança automaticamente para `APPROVED` (gerando
    histórico). Se já estiver em outro status, apenas o `estimatedValue` é atualizado.
  - Um orçamento já aprovado ou rejeitado não pode ser aprovado novamente (`EstimateAlreadyDecidedException`, HTTP 409).
- Uma ordem **não pode ser aprovada** (`APPROVED`, seja pelo `PATCH /status` ou implicitamente ao
  aprovar um orçamento), **iniciar a execução** (`IN_PROGRESS`) **nem ser fechada** (`/close`) sem um
- Uma ordem **não pode iniciar a execução** (`IN_PROGRESS`) **nem ser fechada** (`/close`) sem um
  orçamento aprovado (`EstimateNotApprovedException`, HTTP 422).

---

## Endpoints

| Método | Caminho | Descrição |
|---|---|---|
| `POST` | `/v1/work-orders` | Abre uma nova ordem de serviço. |
| `GET` | `/v1/work-orders/{id}` | Busca uma ordem por id. |
| `GET` | `/v1/work-orders` | Lista ordens, paginado e ordenado por prioridade. |
| `PATCH` | `/v1/work-orders/{id}/status` | Avança o status da ordem (exceto para `COMPLETED`). |
| `POST` | `/v1/work-orders/{id}/estimate` | Cria um orçamento com itens do catálogo de peças. |
| `PATCH` | `/v1/work-orders/{id}/estimate/{estimateId}/approve` | Aprova um orçamento pendente. |
| `POST` | `/v1/work-orders/{id}/services` | Registra uma linha de mão de obra executada. |
| `PATCH` | `/v1/work-orders/{id}/close` | Finaliza a ordem (`IN_PROGRESS` → `COMPLETED`). |

---

## Passo a passo (exemplo completo)

> Os exemplos usam `curl`. Troque `$TOKEN` pelo token JWT (veja o [README.md](README.md#autenticação--gerando-e-usando-o-token-jwt)) se o ambiente exigir autenticação, e os UUIDs pelos valores reais retornados em cada chamada.

### 1. Abrir a ordem de serviço

```shell
curl -s -X POST http://localhost:8080/v1/work-orders \
  -H "Content-Type: application/json" \
  -d '{
        "customerId": "<customer-uuid>",
        "vehicleId": "<vehicle-uuid>",
        "description": "Revisão dos 10.000km",
        "priority": "HIGH"
      }'
```

A ordem é criada com `status = RECEIVED`, `openedAt` preenchido automaticamente. `priority` é opcional —
se omitido, assume `MEDIUM`.

### 2. Avançar para diagnóstico

```shell
curl -s -X PATCH http://localhost:8080/v1/work-orders/<work-order-id>/status \
  -H "Content-Type: application/json" \
  -d '{"status": "DIAGNOSIS"}'
```

### 3. Criar o orçamento

```shell
curl -s -X PATCH http://localhost:8080/v1/work-orders/<work-order-id>/status \
  -H "Content-Type: application/json" -d '{"status": "WAITING_APPROVAL"}'

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

### 4. Aprovar o orçamento

```shell
curl -s -X PATCH http://localhost:8080/v1/work-orders/<work-order-id>/estimate/<estimate-id>/approve
```

A ordem (que estava em `WAITING_APPROVAL`) passa automaticamente para `APPROVED`, e
`estimatedValue` recebe o `totalAmount` do orçamento.

### 5. Iniciar a execução

```shell
curl -s -X PATCH http://localhost:8080/v1/work-orders/<work-order-id>/status \
  -H "Content-Type: application/json" -d '{"status": "IN_PROGRESS"}'
```

Falharia com `422 ESTIMATE_NOT_APPROVED` se não houvesse orçamento aprovado.

### 6. Registrar a mão de obra executada

```shell
curl -s -X POST http://localhost:8080/v1/work-orders/<work-order-id>/services \
  -H "Content-Type: application/json" \
  -d '{"description": "Troca de óleo e filtros", "price": 120.00}'
```

Pode ser chamado várias vezes — cada chamada cria uma linha de `WorkOrderService` independente.

### 7. Fechar a ordem

```shell
curl -s -X PATCH http://localhost:8080/v1/work-orders/<work-order-id>/close \
  -H "Content-Type: application/json" \
  -d '{"finalValue": 410.50}'
```

Exige status `IN_PROGRESS` e orçamento aprovado. `finalValue` é opcional — se omitido, usa o
`estimatedValue` da ordem. Pode ser usado para registrar um valor final diferente do orçamento
aprovado (ex.: serviço extra identificado durante a execução ou desconto concedido); quando
informado, deve ser maior que zero. Define `closedAt` e move o status para `COMPLETED`.

### 8. Entregar o veículo

```shell
curl -s -X PATCH http://localhost:8080/v1/work-orders/<work-order-id>/status \
  -H "Content-Type: application/json" -d '{"status": "DELIVERED"}'
```

A partir daqui a ordem está **bloqueada**: nenhuma alteração de status, orçamento ou serviço é
mais aceita.

### Cancelando uma ordem

Em qualquer ponto antes de `DELIVERED`, a ordem pode ser cancelada:

```shell
curl -s -X PATCH http://localhost:8080/v1/work-orders/<work-order-id>/status \
  -H "Content-Type: application/json" -d '{"status": "CANCELLED"}'
```

---

## Erros mais comuns

| HTTP | Código | Quando acontece |
|---|---|---|
| 404 | `WORK_ORDER_NOT_FOUND` | Id de ordem inexistente. |
| 404 | `ESTIMATE_PART_NOT_FOUND` | `partId` informado no orçamento não existe no catálogo. |
| 404 | `ESTIMATE_NOT_FOUND` | `estimateId` não existe ou não pertence à ordem informada. |
| 409 | `ESTIMATE_ALREADY_DECIDED` | Tentativa de aprovar um orçamento já aprovado/rejeitado. |
| 422 | `WORK_ORDER_LOCKED` | Ordem já `DELIVERED` ou `CANCELLED`. |
| 422 | `INVALID_STATUS_TRANSITION` | Transição de status inválida (pular etapa, ir direto para `COMPLETED`, etc.). |
| 422 | `ESTIMATE_NOT_APPROVED` | Tentativa de iniciar (`IN_PROGRESS`) ou fechar (`/close`) sem orçamento aprovado. |

---

## Cobertura de testes

O fluxo é validado em duas camadas, ambas no pacote `br.com.fiap.postech.soat16.fase1`:

- **Unitário** — `service.WorkOrderServiceTest`: regras de negócio isoladas, repositórios mockados.
  Cobre também transições artificiais (ex.: travas combinadas) difíceis de reproduzir via API.
- **Integração** — `controller.WorkOrderControllerIT`: mesmo fluxo batendo no endpoint HTTP real
  contra um PostgreSQL via Testcontainers (`./mvnw test -Pitest`, requer Docker) — valida
  serialização JSON, mapeamento JPA/Hibernate Reactive e Bean Validation de ponta a ponta.

A tabela liga cada regra já descrita neste documento ao teste que a exercita (nomes abreviados
como `Classe.método`; todas vivem dentro de uma classe `@Nested` com o mesmo nome do endpoint):

| Regra de negócio | Integração (`WorkOrderControllerIT`) | Unitário (`WorkOrderServiceTest`) |
|---|---|---|
| Ordem nasce em `RECEIVED`, `openedAt` automático | `FullLifecycle.shouldCompleteFullLifecycle`, `Create.shouldCreateWorkOrder` | `Create.shouldPersistWorkOrder...` |
| Cliente/veículo inexistente ao criar → 404 | `Create.shouldReturn404WhenCustomerNotFound` / `...VehicleNotFound` | `Create.shouldThrow...NotFoundException` |
| Não é permitido pular etapas (ex.: `RECEIVED` → `APPROVED` direto) | `UpdateStatus.shouldRejectSkippingStages` | `UpdateStatus.shouldRejectSkippingStages` |
| `COMPLETED` só via `/close`, nunca pelo `/status` | `UpdateStatus.shouldRejectCompletedViaGenericEndpoint` | `UpdateStatus.shouldRejectJumpingDirectlyToCompleted` |
| `CANCELLED` a partir de qualquer status não terminal | `FullLifecycle.shouldCancelFromNonTerminalStatus` | `UpdateStatus.shouldAllowCancelledFromAnyNonTerminalStatus` |
| `DELIVERED`/`CANCELLED` bloqueiam qualquer alteração posterior (`WORK_ORDER_LOCKED`) | `FullLifecycle.shouldLockWorkOrderAfterDelivered` | `UpdateStatus.shouldThrowWorkOrderLockedException...`, `CreateEstimate`/`AddService.shouldThrowWorkOrderLockedException...` |
| Orçamento aprovado grava `estimatedValue` na ordem | `ApproveEstimate.shouldApproveAndAdvanceStatus` | `ApproveEstimate.shouldApproveEstimateSetEstimatedValue...` |
| Aprovar orçamento em `WAITING_APPROVAL` avança a ordem para `APPROVED` automaticamente | `ApproveEstimate.shouldApproveAndAdvanceStatus` | `ApproveEstimate.shouldApproveEstimateSetEstimatedValue...AdvanceWaitingApprovalToApproved` |
| Orçamento já decidido não pode ser aprovado de novo → 409 | `ApproveEstimate.shouldReturn409WhenAlreadyDecided` | `ApproveEstimate.shouldThrowEstimateAlreadyDecidedException` |
| Ordem não pode ir para `APPROVED` nem `IN_PROGRESS` sem orçamento aprovado | `UpdateStatus.shouldRejectApprovingWithoutApprovedEstimate` | `UpdateStatus.shouldThrowEstimateNotApprovedException...WhenApproving` / `...StartingExecution` |
| `/close` exige orçamento aprovado e status `IN_PROGRESS` | `Close.shouldReturn422WhenNotInProgress` | `Close.shouldThrow...WhenNotInProgress`, `...WhenThereIsNoApprovedEstimate` |
| `finalValue` opcional no fechamento — usa `estimatedValue` se omitido | `Close.shouldCloseUsingEstimatedValueWhenOmitted` | `Close.shouldCompleteUsingTheApprovedEstimateValueWhenFinalValueIsOmitted` |
| `finalValue` pode divergir do orçamento (serviço extra/desconto) | `Close.shouldCloseUsingProvidedFinalValue` | `Close.shouldUseTheProvidedFinalValueWhenPresent`, `...shouldRecordHistoryTransitionToCompletedWhenFinalValueDiffersFromTheEstimate` |
| Peça inexistente no orçamento → 404 | `CreateEstimate.shouldReturn404WhenPartNotFound` | `CreateEstimate.shouldThrowEstimatePartNotFoundException` |
| Itens de orçamento vazios são rejeitados → 400 | `CreateEstimate.shouldReturn400WhenItemsEmpty` | — (Bean Validation, não exercida no nível de serviço) |
| Mão de obra pode ser registrada múltiplas vezes na mesma ordem | `AddService.shouldAddService` | `AddService.shouldPersistALaborServiceLine` |

Cenários de validação de payload (`@Valid`/Bean Validation, ex.: descrição em branco, preço/valor
zero) só existem na camada de integração — só ali a requisição passa pelo filtro JAX-RS real antes
de chegar ao serviço.
