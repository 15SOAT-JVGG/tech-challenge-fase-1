# fiap-srv-mecanica

Construído com [Quarkus](https://quarkus.io/) (reactive stack), expõe uma API REST para criação, consulta, atualização e remoção de clientes.

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Runtime | Quarkus 3.x (Reactive) |
| Persistência | Hibernate Reactive + Panache + PostgreSQL |
| Observabilidade | OpenTelemetry + Micrometer + Prometheus |
| Mapeamento | MapStruct |

---

## Arquitetura

O serviço segue o padrão **MVC**:

```
controller/        → Endpoints REST (CustomerController + interface OpenAPI)
service/           → Lógica de negócio (CustomerService)
repository/        → Acesso a dados via Panache (CustomerRepository)
model/             → Entidade JPA (Customer) e auditoria
dto/
  request/         → CustomerCreateRequest, CustomerUpdateRequest
  response/        → CustomerResponse, ApiErrorResponse
  pagination/      → PageableRequest, PageableResponse, Pagination
mapper/            → CustomerMapper (MapStruct)
exception/         → AppException (base), exceções específicas, ErrorType/Code
config/            → GlobalExceptionMapper, OpenApiConfig, SchemaInitializer
```

---

## Variáveis de ambiente

| Variável | Descrição | Exemplo         |
|---|---|-----------------|
| `INFRA_OTEL_HOST` | Endpoint do coletor OTEL | `x.x.x.x:14317` |
| `INFRA_HOST_OTEL` | Endpoint OTEL (perfil dev) | `x.x.x.x:14317` |

---

## Executando localmente

### Pré-requisitos

- Java 21+
- Maven 3.9+
- PostgreSQL rodando (via Docker ou homelab)

### Dev mode

```shell
./mvnw quarkus:dev
```

A aplicação sobe em `http://localhost:8080` com live reload habilitado.
O Dev UI fica disponível em `http://localhost:8080/q/dev/`.

---

## Testes

### Unitários

```shell
./mvnw test
```

Executa apenas os testes `*Test.java` sem dependências externas.

### Integração (Testcontainers)

```shell
./mvnw test -Pitest
```

Sobe um container PostgreSQL via Testcontainers e executa testes `*IT.java` + `*Test.java` contra a stack completa.

> Requer Docker em execução.

---

## Análise estática

```shell
./mvnw verify
```

Executa testes, cobertura JaCoCo e a análise estática local:

- Spotless: imports, espaços finais e newline final
- Checkstyle: estilo Java, nomenclatura e imports explícitos
- PMD: regras leves para bugs prováveis e boas práticas de baixo ruído
- SpotBugs: bytecode analysis em classes de aplicação, com prioridade média ou alta
- JaCoCo: cobertura mínima de 70%, ignorando entidades, configs e mappers gerados

Para corrigir automaticamente a camada segura de formatação:

```shell
./mvnw spotless:apply
```

Para rodar apenas os checks estáticos:

```shell
./mvnw spotless:check checkstyle:check pmd:check pmd:cpd-check spotbugs:check
```

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
# ou, sem GraalVM instalado localmente:
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

---

## API

A documentação OpenAPI (Swagger UI) fica disponível em:

```
http://localhost:8090/q/swagger-ui
```

> A porta de management é `8090`, separada da porta da aplicação (`8080`).
