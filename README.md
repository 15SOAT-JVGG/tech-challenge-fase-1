# fiap-srv-oficina-mecanica

Back-end (MVP) do **Sistema Integrado de Atendimento e Execução de Serviços** de uma oficina
mecânica — 15SOAT, Tech Challenge Fase 1. Construído com [Quarkus](https://quarkus.io/), aplica
**Domain-Driven Design**, expõe uma **API REST documentada (Swagger)** e cobre a gestão de
**ordens de serviço, clientes, veículos, serviços, peças/insumos (com controle de estoque) e
orçamentos**, além de um **canal público** para o cliente acompanhar e autorizar o serviço.

---

## Sumário

- [Funcionalidades](#funcionalidades)
- [Tecnologias](#tecnologias)
- [Por que PostgreSQL](#por-que-postgresql)
- [Pré-requisitos](#pré-requisitos)
- [Como rodar (passo a passo)](#como-rodar-passo-a-passo)
- [Autenticação JWT](#autenticação-jwt)
- [Mapa de endpoints](#mapa-de-endpoints)
- [Ciclo de vida da Ordem de Serviço](#ciclo-de-vida-da-ordem-de-serviço)
- [Testando a API (Postman/Newman)](#testando-a-api-postmannewman)
- [Testes automatizados e cobertura](#testes-automatizados-e-cobertura)
- [Análise estática e segurança](#análise-estática-e-segurança)
- [Build](#build)
- [Documentação DDD e relatórios](#documentação-ddd-e-relatórios)

---

## Funcionalidades

- **Ordem de Serviço (OS):** abertura, inclusão de serviços e de peças/insumos, **orçamento gerado
  automaticamente** (peças + mão de obra) e envio ao cliente para aprovação.
- **Acompanhamento:** ciclo de status `RECEIVED → DIAGNOSIS → WAITING_APPROVAL → APPROVED →
  IN_PROGRESS → COMPLETED → DELIVERED` (+ `CANCELLED`), com **transições automáticas** conforme as
  ações (gerar orçamento, aprovar/rejeitar, fechar).
- **Canal público do cliente:** consulta da OS e **aprovação/rejeição do orçamento** via API, sem login.
- **Gestão administrativa:** CRUD de clientes, veículos, serviços e peças; **controle de estoque**
  (baixa na aprovação, restauração no cancelamento, alerta de estoque mínimo).
- **Métrica:** tempo médio de execução das OS concluídas.
- **Segurança:** autenticação JWT (RS256) e RBAC nas APIs administrativas; validação de CPF/CNPJ e placa.

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Runtime | Quarkus 3.x |
| Persistência | Hibernate Reactive + Panache + PostgreSQL 16 |
| Migrations | Flyway |
| Segurança | SmallRye JWT (RS256) + RBAC |
| Documentação | SmallRye OpenAPI / Swagger UI |
| Observabilidade | OpenTelemetry + Micrometer/Prometheus |
| Mapeamento | MapStruct |
| Testes | JUnit 5, REST-assured, Testcontainers |

---

## Por que PostgreSQL

optamos por **PostgreSQL (relacional)** pelos seguintes motivos:

- **Consistência forte e transações ACID** são requisito do domínio: a **baixa de estoque na
  aprovação do orçamento** e a **restauração no cancelamento** precisam ser atômicas (ou tudo é
  aplicado, ou nada) para não corromper o saldo de peças.
- **Modelo fortemente relacional:** cliente → veículo → OS → (serviços, orçamento → itens → peças)
  e histórico de status. Integridade referencial e *joins* são naturais em SQL.
- **Invariantes e unicidade** (CPF/CNPJ e placa únicos) resolvidas com *constraints* no banco.
- **Maturidade e custo:** open-source, amplamente adotado, com excelente suporte no ecossistema
  Quarkus (driver reativo `reactive-pg-client` + JDBC para Flyway) e em Kubernetes.

NoSQL não traria vantagem aqui: o volume de um MVP de oficina é moderado e os dados são
relacionais e transacionais — um banco documental adicionaria complexidade de consistência sem ganho.

---

## Pré-requisitos

- **Para subir a aplicação:** apenas **Docker + Docker Compose**. Nada mais — nem Java, nem Maven, nem `.env`.
- **Para o dev mode ou rodar os testes localmente:** **Java 21+** e **Maven 3.9+** (ou o wrapper `./mvnw`).

---
| Variável | Descrição | Padrão |
|---|---|---|
| `INFRA_HOST_POSTGRES` | Host do PostgreSQL | `localhost` |
| `POSTGRES_DB` | Nome do **banco** | `oficina` |
| `POSTGRES_USERNAME` | Usuário do banco | `admin` |
| `POSTGRES_PASSWORD` | Senha do banco | `admin` |
| `APP_SEED_ADMIN_USERNAME` / `APP_SEED_ADMIN_PASSWORD` | Admin inicial | `admin` / `admin123` |
| `APP_SEED_MECHANIC_USERNAME` / `APP_SEED_MECHANIC_PASSWORD` | Mecânico inicial | `mecanico` / `mecanico123` |
| `JWT_EXPIRATION_HOURS` | Validade do token (horas) | `8` |

> **Banco x Schema:** o **banco** chama-se `oficina` e a aplicação cria/usa o **schema**
> `oficina_mecanica` automaticamente na inicialização (Flyway + Hibernate). Não confunda os dois.
>
> **Senhas de seed:** via `docker compose` já vêm preenchidas (`admin123` / `mecanico123`). Se você
> rodar fora do Compose com `APP_SEED_*_PASSWORD` em branco, a aplicação **gera uma senha aleatória**
> no primeiro boot e a imprime no log com o prefixo `[SEED]`.

---

## Como rodar (passo a passo)

### Um único comando (recomendado)

Na raiz do projeto, com o Docker em execução:

```shell
docker compose up
```

É só isso. O Compose **constrói a aplicação a partir do código-fonte** (build multistage, dentro do
próprio Docker) e sobe **aplicação + PostgreSQL** já configurados — não é preciso instalar Java/Maven,
rodar `mvnw package` antes, nem criar `.env`. Na primeira execução o build baixa as dependências e
leva alguns minutos; nas próximas é quase instantâneo (cache).

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8090/q/swagger-ui`
- Login: **`admin` / `admin123`** (ver [Autenticação JWT](#autenticação-jwt))

| Ação | Comando |
|---|---|
| Subir em segundo plano | `docker compose up -d` |
| Acompanhar os logs | `docker compose logs -f app` |
| Derrubar | `docker compose down` |
| Derrubar e apagar o banco | `docker compose down -v` |

> O `.env` é **opcional**: todos os valores já vêm preenchidos no `docker-compose.yml`. Crie um `.env`
> (a partir de `.env.example`) **somente** se quiser sobrescrever portas, senhas ou nomes padrão.

### Alternativa — Dev mode (live reload)

Requer Java 21 + Maven. Sobe só o banco em container e a aplicação localmente com hot reload:

```shell
docker compose up -d postgres
./mvnw quarkus:dev
```

- API em `http://localhost:8080` (live reload) · Dev UI em `http://localhost:8080/q/dev/`.

---

## Autenticação JWT

As APIs administrativas usam **Bearer JWT (RS256)**: login → token → header `Authorization`.

| Usuário | Role | Senha (com `.env.example`) |
|---|---|---|
| `admin` | `ADMIN` | `admin123` |
| `mecanico` | `MECHANIC` | `mecanico123` |

### 1. Login

```shell
curl -s -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Resposta:

```json
{ "token": "eyJraWQiOiJ...", "username": "admin", "role": "ADMIN", "expiresIn": 28800 }
```

### 2. Usando o token

```shell
TOKEN="eyJraWQiOiJ..."
curl -s http://localhost:8080/v1/customer -H "Authorization: Bearer $TOKEN"
```

### 3. Pelo Swagger UI

Abra `http://localhost:8090/q/swagger-ui` → **Authorize** (esquema `bearerAuth`) → cole **apenas o
token** (sem `Bearer`) → as chamadas passam a enviar o header automaticamente.

> Sem senha definida no `.env`? Recupere a gerada no log:
> `docker compose logs app | grep SEED`

---

## Mapa de endpoints

Base: `http://localhost:8080`. Papéis: 🔓 público · 🔧 ADMIN+MECHANIC · 🛡️ ADMIN.

> **Modelo de acesso (segregação de funções):** os **cadastros/dados-mestre** (clientes, veículos,
> serviços, peças e workers) são **escritos apenas pelo ADMIN**; o MECHANIC pode **consultá-los**
> (GET) mas não criar/editar/excluir. As **Ordens de Serviço** (abrir, mudar status, lançar serviço,
> orçamento, fechar) são operadas por **ADMIN e MECHANIC**. O ADMIN é superusuário (faz tudo).

### Autenticação — `/v1/auth`
| Método | Path | Acesso |
|---|---|---|
| POST | `/v1/auth/login` | 🔓 |

### Clientes — `/v1/customer`
| Método | Path | Acesso |
|---|---|---|
| GET | `/v1/customer` | 🔧 |
| GET | `/v1/customer/{id}` | 🔧 |
| GET | `/v1/customer/by-document/{document}` | 🔧 |
| POST | `/v1/customer` | 🛡️ |
| PUT | `/v1/customer/{id}` | 🛡️ |
| DELETE | `/v1/customer/{id}` | 🛡️ |

### Veículos — `/v1/vehicle`
| Método | Path | Acesso |
|---|---|---|
| GET | `/v1/vehicle` (filtros: `license_plate`, `manufacturer`, `model`) | 🔧 |
| GET | `/v1/vehicle/{id}` | 🔧 |
| GET | `/v1/vehicle/by-license-plate/{license_plate}` | 🔧 |
| POST | `/v1/vehicle` | 🛡️ |
| PUT | `/v1/vehicle/{id}` | 🛡️ |
| DELETE | `/v1/vehicle/{id}` | 🛡️ |

### Trabalhadores — `/v1/worker`
| Método | Path | Acesso |
|---|---|---|
| GET | `/v1/worker` · `/v1/worker/{id}` | 🛡️ |
| POST | `/v1/worker` | 🛡️ |
| POST | `/v1/worker/login` | 🔓 |
| PUT | `/v1/worker/{id}` | 🛡️ |
| DELETE | `/v1/worker/{id}` | 🛡️ |

### Catálogo de serviços — `/admin/services`
| Método | Path | Acesso |
|---|---|---|
| GET | `/admin/services` · `/admin/services/{id}` | 🔧 |
| POST | `/admin/services` | 🛡️ |
| PUT | `/admin/services/{id}` | 🛡️ |
| DELETE | `/admin/services/{id}` | 🛡️ |

### Peças e insumos — `/admin/parts`
| Método | Path | Acesso |
|---|---|---|
| GET | `/admin/parts` · `/admin/parts/{id}` · `/admin/parts/low-stock` | 🔧 |
| POST | `/admin/parts` | 🛡️ |
| PUT | `/admin/parts/{id}` | 🛡️ |
| PATCH | `/admin/parts/{id}/stock?adjustment=±N` | 🛡️ |
| DELETE | `/admin/parts/{id}` | 🛡️ |

### Ordens de serviço — `/v1/work-orders`
| Método | Path | Acesso |
|---|---|---|
| GET | `/v1/work-orders` (`q`, `page`, `size`) | 🔧 |
| GET | `/v1/work-orders/{id}` | 🔧 |
| GET | `/v1/work-orders/metrics/average-execution-time` | 🛡️ |
| POST | `/v1/work-orders` | 🔧 |
| POST | `/v1/work-orders/{id}/services` | 🔧 |
| POST | `/v1/work-orders/{id}/estimate` | 🔧 |
| PATCH | `/v1/work-orders/{id}/status` | 🔧 |
| PATCH | `/v1/work-orders/{id}/estimate/{estimateId}/approve` | 🔧 |
| PATCH | `/v1/work-orders/{id}/estimate/{estimateId}/reject` | 🔧 |
| PATCH | `/v1/work-orders/{id}/close` | 🔧 |

### Canal público do cliente — `/v1/public/work-orders`
| Método | Path | Acesso |
|---|---|---|
| GET | `/v1/public/work-orders/{id}` | 🔓 |
| PATCH | `/v1/public/work-orders/{id}/estimate/{estimateId}/approve` | 🔓 |
| PATCH | `/v1/public/work-orders/{id}/estimate/{estimateId}/reject` | 🔓 |

---

## Ciclo de vida da Ordem de Serviço

```
RECEIVED ─► DIAGNOSIS ─► WAITING_APPROVAL ─► APPROVED ─► IN_PROGRESS ─► COMPLETED ─► DELIVERED
                                   │
                                   └─(rejeição)─► DIAGNOSIS     (qualquer não-terminal) ─► CANCELLED
```

- A OS nasce em **`RECEIVED`** (status definido automaticamente na criação).
- Gerar o orçamento move `DIAGNOSIS → WAITING_APPROVAL`; aprovar move `→ APPROVED` (e dá baixa no
  estoque); rejeitar volta para `DIAGNOSIS`.
- `COMPLETED` só via `PATCH /{id}/close` (exige OS `IN_PROGRESS` e orçamento aprovado).
- `CANCELLED` em `APPROVED` restaura o estoque reservado. OS `DELIVERED`/`CANCELLED` ficam bloqueadas.

Detalhes em [WORKORDER.md](WORKORDER.md).

---

## Testando a API (Postman/Newman)

Uma **collection E2E** cobre todos os endpoints em ordem executável (auth → CRUDs → ciclo completo
da OS → canal público → segurança). É auto-contida (gera tokens, CPFs e placas válidos
dinamicamente). Ver [postman/README.md](postman/README.md).

```powershell
# Newman via Docker (API no ar), Windows/PowerShell:
docker run --rm --add-host=host.docker.internal:host-gateway `
  -v "${PWD}\postman:/etc/newman" -t postman/newman:latest `
  run /etc/newman/Oficina-Mecanica-E2E.postman_collection.json `
  --env-var base_url=http://host.docker.internal:8080
```

---

## Testes automatizados e cobertura

```shell
./mvnw test            # unitários (*Test.java), sem dependências externas
./mvnw test -Pitest    # + integração (*IT.java) com Testcontainers — requer Docker
./mvnw verify          # testes + cobertura (JaCoCo) + análise estática
```

Cobertura medida pelo gate JaCoCo; relatório em `target/jacoco-report/index.html`.

---

## Análise estática e segurança

`./mvnw verify` executa Spotless, Checkstyle, PMD, SpotBugs e JaCoCo.

A esteira de CI (`.github/workflows/ci.yml`) adiciona varreduras de segurança: **Gitleaks**
(secrets), **Semgrep** (SAST), **OWASP Dependency-Check** (SCA) e **SonarQube**. O resultado
consolidado está em [docs/RELATORIO-VULNERABILIDADES.md](docs/RELATORIO-VULNERABILIDADES.md) e o
relatório de dependências em `dependency-check-report.zip`.

> **Chaves JWT:** o par RSA em `src/main/resources/jwt` é **somente para desenvolvimento**. Em
> produção, aponte `JWT_PRIVATE_KEY_LOCATION` / `JWT_PUBLIC_KEY_LOCATION` para secrets montados e
> **nunca** use as chaves versionadas. Em produção, defina também `SWAGGER_UI_ENABLED=false`.

---

## Build

```shell
# JAR
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar

# Nativo (GraalVM ou container)
./mvnw package -Dnative
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

---

## Documentação DDD e relatórios

- **Event Storming, Bounded Contexts, Linguagem Ubíqua e Modelo de Dados:** board no Miro
  (ver documento de entrega).
- **Fluxo da OS:** [WORKORDER.md](WORKORDER.md).
- **Vulnerabilidades:** [docs/RELATORIO-VULNERABILIDADES.md](docs/RELATORIO-VULNERABILIDADES.md).
- **Collection da API:** [postman/]
