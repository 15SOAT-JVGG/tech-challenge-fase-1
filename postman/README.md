# Collection Postman — Oficina Mecânica API (E2E)

`Oficina-Mecanica-E2E.postman_collection.json` cobre os 38 endpoints da API em 10 pastas, executadas em ordem:

| Pasta | Cobertura |
|---|---|
| 00 - Auth | Login ADMIN/MECHANIC, credenciais inválidas |
| 01 - Customers | CRUD com **escrita só ADMIN** (MECHANIC apenas lê); RBAC (create/delete por MECHANIC → 403), validação, documento duplicado/inválido |
| 02 - Vehicles | CRUD com **escrita só ADMIN** (MECHANIC apenas lê); filtro por fabricante, placa inválida, cliente inexistente, RBAC (create/delete por MECHANIC → 403) |
| 03 - Workers | CRUD (ADMIN-only), login de worker (não gera JWT — só valida credenciais) |
| 04 - Parts and Supplies | CRUD, ajuste de estoque, low-stock, RBAC (mutação só ADMIN) |
| 05 - Service Catalog | CRUD, RBAC (mutação só ADMIN) |
| 06 - Work Orders - Happy Path | Ciclo completo: RECEIVED → DIAGNOSIS → orçamento → aprovação → IN_PROGRESS → fechamento → DELIVERED, métricas |
| 07 - Work Orders - Rejection and Public Channel | Rejeição de orçamento, novo orçamento, aprovação via canal público (sem auth) |
| 08 - Work Orders - Cancellation and Locking | Cancelamento e bloqueio de OS cancelada |
| 09 - Cross-cutting Security | 401 sem token, 404 para recurso inexistente |

## Como rodar

A API precisa estar no ar (`docker compose up`, ver README principal).

**Opção A — Postman GUI:** importe o arquivo, ajuste a variável de coleção `base_url` se necessário (default `http://localhost:8080`), rode com o Collection Runner em ordem de pasta.

**Opção B — Newman via Docker (sem instalar nada):**

No Windows, rode no **PowerShell** (mais confiável — sem problemas de tradução de caminho):

```powershell
cd C:\dev\git\fiap\tech-challenge
docker run --rm --add-host=host.docker.internal:host-gateway `
  -v "${PWD}\postman:/etc/newman" -t postman/newman:latest `
  run /etc/newman/Oficina-Mecanica-E2E.postman_collection.json `
  --env-var base_url=http://host.docker.internal:8080
```

No Linux/macOS (bash):

```shell
docker run --rm --add-host=host.docker.internal:host-gateway \
  -v "$(pwd)/postman:/etc/newman" -t postman/newman:latest \
  run /etc/newman/Oficina-Mecanica-E2E.postman_collection.json \
  --env-var base_url=http://host.docker.internal:8080
```

## Notas

- A collection é auto-contida: tokens e ids são gerados e propagados via variáveis de coleção (CPF e placa válidos são gerados dinamicamente nos pre-request scripts).
- Pode ser executada repetidamente sem reset do banco — usa dados aleatórios e CPFs/placas únicos a cada run.
- `List Work Orders (capture id)` busca a OS recém-criada filtrando por `description` (os endpoints de criação de Customer/Vehicle/Worker/WorkOrder não retornam corpo), com `size=100`. Em bases com muitas OS acumuladas de runs antigos, isso pode falhar — nesse caso, considere limpar o banco antes de rodar.
