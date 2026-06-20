# fiap-srv-mecanica

Back-end de uma oficina mecânica (15SOAT — Tech Challenge Fase 1): gestão de **clientes, veículos, peças/insumos e catálogo de serviços**, com o fluxo completo de **ordens de serviço** — abertura, diagnóstico, orçamento (peças + mão de obra), aprovação/rejeição pelo cliente (inclusive por um canal público, sem autenticação) e execução até a entrega. Construído com [Quarkus](https://quarkus.io/) (reactive stack) e **autenticação JWT** nas APIs administrativas.

Veja também [WORKORDER.md](WORKORDER.md) para o detalhamento da máquina de estados e das regras de negócio da ordem de serviço.

**Documentação DDD** (Event Storming, diagramas e Linguagem Ubíqua): [Miro](https://miro.com/app/board/uXjVHbbU2eE=/?share_link_id=729083750980).

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Runtime | Quarkus 3.x (Reactive) |
| Persistência | Hibernate Reactive + Panache + PostgreSQL |
| Segurança | SmallRye JWT (RS256) |
| Observabilidade | OpenTelemetry + Micrometer + Prometheus |
| Mapeamento | MapStruct |

### Por que PostgreSQL

O domínio é predominantemente **relacional e transacional**: clientes, veículos, ordens de serviço, orçamentos, itens de orçamento e peças se relacionam por chaves estrangeiras (ex.: uma OS referencia cliente e veículo; um orçamento referencia a OS e suas peças), e a baixa de estoque na aprovação do orçamento (`Part.decreaseStock`) precisa de garantias **ACID** dentro da mesma transação que aprova o orçamento — uma falha não pode deixar estoque decrementado sem o orçamento de fato aprovado, nem vice-versa. Um banco de documentos (ex.: MongoDB) exigiria modelar manualmente essas consistências que um relacional já garante nativamente.

Dentro do universo de bancos relacionais, o PostgreSQL foi escolhido por:

- **Suporte de primeira classe no ecossistema Quarkus** — `quarkus-reactive-pg-client` oferece um driver reativo não-bloqueante nativo (sem adaptador JDBC por trás de um pool de threads), o que importa porque toda a stack de persistência do projeto é Hibernate Reactive + Mutiny;
- **Maturidade, gratuidade e ampla adoção** — sem custo de licença, fácil de provisionar localmente (Docker) ou em qualquer provedor cloud;
- **Tipagem nativa de `UUID`** — todas as entidades usam UUID como chave primária (`GenerationType.UUID`); o Postgres trata isso como tipo de coluna nativo, sem precisar de `CHAR(36)`/`BINARY(16)` como em outros bancos.

---

## Portas

| Porta | Uso |
|---|---|
| `8080` | API da aplicação (endpoints `/v1/**`) |
| `8090` | Management — Swagger UI, OpenAPI, health e métricas |

- Swagger UI: `http://localhost:8090/q/swagger-ui`
- OpenAPI: `http://localhost:8090/q/openapi`

---

## Pré-requisitos

- Java 21+
- Maven 3.9+ (ou o wrapper `./mvnw`)
- Docker + Docker Compose (para o banco e/ou execução completa)

---

## Configuração (.env)

Copie o arquivo de exemplo e ajuste as variáveis:

```shell
cp .env.example .env
```

Variáveis relevantes:

| Variável | Descrição | Padrão |
|---|---|---|
| `INFRA_HOST_POSTGRES` | Host do PostgreSQL | `localhost` |
| `POSTGRES_DB` | Nome do banco (deve existir) | `oficina_mecanica` |
| `POSTGRES_USERNAME` | Usuário do banco | `admin` |
| `POSTGRES_PASSWORD` | Senha do banco | — (obrigatória no Compose) |
| `APP_SEED_ADMIN_USERNAME` | Usuário admin inicial | `admin` |
| `APP_SEED_ADMIN_PASSWORD` | Senha do admin inicial | — (ver nota abaixo) |
| `APP_SEED_MECHANIC_USERNAME` | Usuário mecânico inicial | `mecanico` |
| `APP_SEED_MECHANIC_PASSWORD` | Senha do mecânico inicial | — (ver nota abaixo) |
| `JWT_EXPIRATION_HOURS` | Validade do token (horas) | `8` |

> **Importante (token):** se `APP_SEED_ADMIN_PASSWORD` / `APP_SEED_MECHANIC_PASSWORD` ficarem em branco, a aplicação **gera uma senha aleatória** na primeira inicialização e a imprime no log com o prefixo `[SEED]`. Para ter credenciais conhecidas (e conseguir gerar tokens sem ler o log), **defina essas senhas no `.env`**. O `.env.example` já traz valores de exemplo (`admin123` / `mecanico123`).

---

## Como rodar localmente

Há dois caminhos. Escolha um.

### Opção A — Ambiente completo via Docker Compose (recomendado)

Sobe PostgreSQL **e** a aplicação juntos. A imagem da aplicação é montada a partir dos artefatos já compilados, então é preciso empacotar antes:

```shell
# 1. Garanta o .env (com POSTGRES_PASSWORD e as senhas de seed definidas)
cp .env.example .env

# 2. Gere um par de chaves JWT local (ver "Segurança das chaves JWT" abaixo —
#    a app roda em modo produção no container e rejeita as chaves de dev versionadas)
mkdir -p .local-jwt
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out .local-jwt/privateKey.pem
openssl rsa -pubout -in .local-jwt/privateKey.pem -out .local-jwt/publicKey.pem

# 3. Compile os artefatos
./mvnw package -DskipTests

# 4. Suba o ambiente
docker compose --env-file .env -f src/main/docker/compose/docker-compose.yml up -d --build
```

A API ficará em `http://localhost:8080` e o Swagger UI em `http://localhost:8090/q/swagger-ui`.

Para derrubar:

```shell
docker compose -f src/main/docker/compose/docker-compose.yml down
```

### Opção B — Dev mode (live reload) + Postgres em container

Útil para desenvolvimento. Suba só o banco e rode a aplicação em modo dev:

```shell
# 1. Apenas o PostgreSQL
docker compose --env-file .env -f src/main/docker/compose/docker-compose.yml up -d postgres

# 2. Aplicação em dev mode (carrega as variáveis do .env)
./mvnw quarkus:dev
```

A aplicação sobe em `http://localhost:8080` com live reload. Dev UI em `http://localhost:8080/q/dev/`.

> As senhas de seed são lidas do `.env`. Em dev mode, exporte as variáveis ou rode com `--env-file` se o seu shell não carregar o `.env` automaticamente.

---

## Autenticação — gerando e usando o token JWT

As APIs administrativas usam **Bearer JWT (RS256)**. O fluxo é: fazer login → receber o token → enviá-lo no header `Authorization`.

### Usuários iniciais (seed)

Criados automaticamente na primeira inicialização:

| Usuário | Role | Senha |
|---|---|---|
| `admin` | `ADMIN` | valor de `APP_SEED_ADMIN_PASSWORD` |
| `mecanico` | `MECHANIC` | valor de `APP_SEED_MECHANIC_PASSWORD` |

Usando o `.env.example`: **`admin` / `admin123`**.

### 1. Login (gera o token)

`POST http://localhost:8080/v1/auth/login`

```shell
curl -s -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

No PowerShell:

```powershell
$resp = Invoke-RestMethod -Method Post -Uri http://localhost:8080/v1/auth/login `
  -ContentType "application/json" `
  -Body '{"username":"admin","password":"admin123"}'
$token = $resp.token
$token
```

Resposta:

```json
{
  "token": "eyJraWQiOiJ...",
  "username": "admin",
  "role": "ADMIN",
  "expiresIn": 28800
}
```

- `token` — JWT assinado a ser usado nas chamadas seguintes.
- `expiresIn` — validade em **segundos** (8h = 28800 por padrão).

### 2. Usando o token nas chamadas

Envie o token no header `Authorization: Bearer <token>`:

```shell
TOKEN="eyJraWQiOiJ..."   # cole o token retornado no login

curl -s http://localhost:8080/v1/vehicle/1 \
  -H "Authorization: Bearer $TOKEN"
```

No PowerShell (reaproveitando `$token` do passo anterior):

```powershell
Invoke-RestMethod -Uri http://localhost:8080/v1/vehicle/1 `
  -Headers @{ Authorization = "Bearer $token" }
```

### 3. Usando o token pelo Swagger UI

1. Abra `http://localhost:8090/q/swagger-ui`.
2. Chame `POST /v1/auth/login` com `admin` / `admin123` e copie o campo `token` da resposta.
3. Clique em **Authorize** (cadeado, esquema `bearerAuth`), cole **apenas o token** (sem o prefixo `Bearer`) e confirme.
4. As demais chamadas passam a enviar o header automaticamente.

> Se você não definiu as senhas no `.env`, recupere a senha gerada no log de inicialização:
> ```
> [SEED] Senha não configurada para 'admin'. Senha gerada (anote e altere): <senha>
> ```
> No Compose: `docker compose -f src/main/docker/compose/docker-compose.yml logs srv-oficina-mecanica | grep SEED`.

---

## Testes

### Unitários

```shell
./mvnw test
```

Executa apenas os testes `*Test.java`, sem dependências externas.

### Integração (Testcontainers)

```shell
./mvnw test -Pitest
```

Sobe um PostgreSQL via Testcontainers e executa `*IT.java` + `*Test.java` contra a stack completa. **Requer Docker em execução.**

---

## Análise estática

```shell
./mvnw verify
```

Executa testes, cobertura JaCoCo e a análise estática local:

- **Spotless** — imports, espaços finais e newline final
- **Checkstyle** — estilo, nomenclatura e imports explícitos
- **PMD** — bugs prováveis e boas práticas
- **SpotBugs** — análise de bytecode (prioridade média/alta)
- **JaCoCo** — cobertura mínima configurada

Corrigir formatação automaticamente:

```shell
./mvnw spotless:apply
```

Apenas os checks estáticos:

```shell
./mvnw spotless:check checkstyle:check pmd:check pmd:cpd-check spotbugs:check
```

Relatórios em `target/`: `surefire-reports/`, `jacoco-report/index.html`, `checkstyle-result.xml`, `reports/pmd.html`, `spotbugsXml.xml`.

---

## Build

### JAR

```shell
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

### Native

```shell
./mvnw package -Dnative
# ou, sem GraalVM local:
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

---

## Segurança das chaves JWT

O par RSA versionado em `src/main/resources/jwt` é **somente para desenvolvimento**. Em produção, sobrescreva `JWT_PRIVATE_KEY_LOCATION` / `JWT_PUBLIC_KEY_LOCATION` apontando para secrets montados (ex.: `file:/etc/jwt/privateKey.pem`) e **nunca** utilize as chaves versionadas.

Um `JwtKeyStartupGuard` falha o boot caso detecte a chave de dev (`jwt/privateKey.pem`) enquanto a app roda em `LaunchMode.NORMAL` — que é como ela sempre roda dentro do container (mesmo localmente, via Docker Compose). Por isso a **Opção A** acima pede para gerar um par de chaves local antes de subir o Compose:

```shell
mkdir -p .local-jwt
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out .local-jwt/privateKey.pem
openssl rsa -pubout -in .local-jwt/privateKey.pem -out .local-jwt/publicKey.pem
```

O `.local-jwt/` é gitignorado e montado como volume read-only em `/etc/jwt` pelo `docker-compose.yml`, que já aponta `JWT_PRIVATE_KEY_LOCATION` / `JWT_PUBLIC_KEY_LOCATION` para esse caminho. Em dev mode (Opção B, sem container) a app continua usando a chave versionada normalmente, pois aí o `LaunchMode` não é `NORMAL`.
