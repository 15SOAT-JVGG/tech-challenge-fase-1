# Collection Postman — Oficina Mecânica API (E2E)

`Oficina-Mecanica-E2E.postman_collection.json` reúne 99 requisições em 10 pastas, executadas em ordem:

| Pasta | Cobertura |
|---|---|
| 00 - Auth | Login ADMIN/MECHANIC, credenciais inválidas |
| 01 - Customers | CRUD com **escrita só ADMIN** (MECHANIC apenas lê); RBAC (create/delete por MECHANIC → 403), validação, documento duplicado/inválido |
| 02 - Vehicles | CRUD com **escrita só ADMIN** (MECHANIC apenas lê); filtro por fabricante, placa inválida, cliente inexistente, RBAC (create/delete por MECHANIC → 403) |
| 03 - Workers | CRUD (ADMIN-only), login de worker (não gera JWT — só valida credenciais) |
| 04 - Parts and Supplies | CRUD, ajuste de estoque, low-stock, RBAC (mutação só ADMIN) |
| 05 - Service Catalog | CRUD, RBAC (mutação só ADMIN) |
| 06 - Work Orders - Happy Path | Abertura com solicitação inicial (orçamento pendente atômico) e ciclo completo: RECEIVED → DIAGNOSIS → orçamento → aprovação → IN_PROGRESS → fechamento → DELIVERED, métricas |
| 07 - Ordens de Serviço - Canal Público | Acompanhamento e aprovação de orçamento pelo canal público (sem auth) |
| 08 - Ordens de Serviço - Recusa de Orçamento e Bloqueio | Recusa de orçamento conclui a OS com `cancelledAt` e bloqueia novas mutações com `WORK_ORDER_LOCKED` |
| 09 - Cross-cutting Security | 401 sem token, 404 para recurso inexistente |

## Como rodar

A API precisa estar no ar (`docker compose up`, ver README principal).

**Opção A — Postman GUI:** importe o arquivo, ajuste a variável de coleção `base_url` se necessário (default `http://localhost:8080`), rode com o Collection Runner em ordem de pasta.

**Opção B — Newman via Docker (sem instalar nada):**

No Windows, rode no **PowerShell** (mais confiável — sem problemas de tradução de caminho):

```powershell
cd C:\dev\git\fiap\tech-challenge
docker run --rm --network=tech-challenge_oficina_mecanica_net `
  -v "${PWD}\postman:/etc/newman" -t postman/newman:latest `
  run /etc/newman/Oficina-Mecanica-E2E.postman_collection.json `
  --env-var base_url=http://srv-oficina-mecanica:8080
```

No Linux/macOS (bash):

```shell
docker run --rm --network=tech-challenge_oficina_mecanica_net \
  -v "$(pwd)/postman:/etc/newman" -t postman/newman:latest \
  run /etc/newman/Oficina-Mecanica-E2E.postman_collection.json \
  --env-var base_url=http://srv-oficina-mecanica:8080
```

## Notas

- A collection é auto-contida: tokens e ids são gerados e propagados via variáveis de coleção (CPF e placa válidos são gerados dinamicamente nos pre-request scripts).
- Pode ser executada repetidamente sem reset do banco — usa dados aleatórios e CPFs/placas únicos a cada run.
- A abertura de OS devolve o id no corpo do `201`, então as requisições de criação capturam `work_order_id` direto da resposta. As requisições de listagem apenas conferem que a OS recém-aberta aparece na fila, filtrando por `description` com `size=100`. Em bases com muitas OS acumuladas de runs antigos, essa conferência pode falhar — nesse caso, considere limpar o banco antes de rodar.
- Os endpoints de criação de Customer/Vehicle/Worker continuam sem corpo de resposta: os ids são capturados pelas buscas por documento/placa/login.
