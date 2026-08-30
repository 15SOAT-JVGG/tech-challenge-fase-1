# Relatório de Análise de Vulnerabilidades

**Projeto:** tech-challenge-fase-1 (16SOAT — Tech Challenge Fase 1)
**Repositório:** `16SOAT-JVGG/tech-challenge`
**Escopo:** back-end monolítico (Quarkus 3.x), pipeline CI e dependências de terceiros.

Este documento consolida o resultado dos scans de segurança executados sobre o código e
as dependências do projeto, conforme exigido nos entregáveis da Fase 1. As varreduras são
executadas automaticamente na esteira de CI a cada Pull Request para a `main`
(`.github/workflows/ci.yml`).

---

## 1. Ferramentas e cobertura

| Categoria | Ferramenta | O que cobre | Gate (falha o build) |
|---|---|---|---|
| Secrets | **Gitleaks** 8.24.3 | Credenciais/segredos no histórico do Git | Achados reportados |
| SAST | **Semgrep** (`p/ci`) | Padrões inseguros no código-fonte (injeção, criptografia fraca, etc.) | `fail_on_findings: true` |
| SCA | **OWASP Dependency-Check** | CVEs conhecidas em dependências (NVD) | `fail_cvss: 8` (falha em CVSS ≥ 8) |
| Qualidade/Segurança | **SonarQube** | Vulnerabilities, Security Hotspots, code smells, cobertura | Quality Gate |

> Defesa em profundidade: secrets → código (SAST) → dependências (SCA) → qualidade contínua (Sonar).

---

## 2. Resultado das varreduras

### 2.1 SCA — OWASP Dependency-Check
- **Resultado: nenhuma dependência vulnerável.** O relatório gerado
  (`dependency-check-report.zip`, anexo na raiz do repositório) **não apresenta CVEs**
  (0 dependências marcadas como *Vulnerable*).
- Política: o build falha automaticamente para qualquer CVE com **CVSS ≥ 8**.

### 2.2 Secrets — Gitleaks
- Varredura de todo o histórico (`fetch_depth: 0`). Sem segredos commitados.
- O par de chaves RS256 **não é versionado nem embutido na imagem**: é gerado uma única vez fora do
  build e informado por `JWT_PRIVATE_KEY_LOCATION` / `JWT_PUBLIC_KEY_LOCATION` — Secret montado em
  produção, volume do Docker no Compose, par efêmero nos testes de integração. O par de
  desenvolvimento que antes vivia em `src/main/resources/jwt` foi removido; como ele permanece no
  histórico do Git, deve ser considerado comprometido e nunca reutilizado.

### 2.3 SAST — Semgrep
- Regras `p/ci` com `fail_on_findings: true` — o merge na `main` só ocorre sem achados bloqueantes.

### 2.4 SonarQube
- Quality Gate aguardado no pipeline (`wait_quality_gate: true`), incluindo Security Hotspots
  e cobertura (relatório JaCoCo XML publicado).

---

## 3. Registro de riscos (análise manual)

Além dos scans automatizados, a revisão arquitetural identificou os riscos abaixo.

### R-01 — Canal público da OS sem verificação de propriedade (Broken Access Control / IDOR) — **tratado**
- **Severidade:** Média · **OWASP Top 10:** A01:2021 (Broken Access Control) · **OWASP ASVS:** V4.2
- **Componente:** `PublicWorkOrderController` (`/v1/public/work-orders/**`, `@PermitAll`).
- **Descrição:** os endpoints públicos de acompanhamento e de **aprovação/rejeição de orçamento**
  autorizavam a operação apenas com base no **UUID da OS** informado no path. Não havia autenticação
  do cliente nem vínculo verificável entre quem chamava e o dono da OS: quem obtivesse o UUID
  (logs, histórico de navegador, encaminhamento de link) visualizava os dados completos da OS e
  **aprovava ou rejeitava o orçamento** de qualquer ordem.
- **Tratamento:** as rotas por id foram **removidas** (hoje respondem `404`) e substituídas por links
  assinados enviados ao cliente por e-mail, conforme
  [ADR-0005](adr/0005-reserva-de-estoque-e-decisao-do-cliente-por-link-assinado.md) e
  [ADR-0006](adr/0006-acompanhamento-publico-por-link-assinado-sem-registro.md):
  - o **acompanhamento** exige um token RS256 com a data de emissão assinada, válido por trinta dias,
    e responde apenas o andamento do atendimento — sem valores, peças, descrição ou mecânico;
  - a **decisão** exige um token RS256 gravado no banco, de **uso único** (`consumedAt`) e válido por
    sete dias, apresentado num `POST` para que clientes de e-mail que pré-carregam links não
    consumam a decisão. Link forjado responde `400`; expirado ou já usado, `410`.
- **Risco residual:** o link continua sendo um *capability token* — quem receber o e-mail encaminhado
  acompanha a OS até o fim do prazo. É o que sustenta a resposta mínima do acompanhamento.
- **Recomendação remanescente:** registrar em audit log as decisões tomadas pelo canal do cliente.

### R-02 — Swagger UI exposto no artefato empacotado
- **Severidade:** Baixa · **OWASP Top 10:** A05:2021 (Security Misconfiguration)
- **Descrição:** o Swagger UI fica acessível também no artefato empacotado durante a fase de
  desenvolvimento (`always-include: ${SWAGGER_UI_ENABLED:true}`), expondo a superfície da API.
- **Atenuante/Recomendação:** já previsto — em produção definir `SWAGGER_UI_ENABLED=false`.

---

## 4. Controles de segurança já implementados

- **Autenticação JWT (RS256)** com chaves assimétricas sempre externas ao artefato — a imagem não
  contém chave privada e o `JwtKeyStartupGuard` impede a subida em produção com a chave local;
  emissor validado.
- **Autorização RBAC** por endpoint (`@RolesAllowed("ADMIN" | "MECHANIC")`).
- **Canal do cliente por link assinado:** nada é acessível por id; o acompanhamento é somente leitura
  e a decisão sobre o orçamento é de uso único, com prazo próprio para cada uso.
- **Negação por padrão:** `quarkus.security.jaxrs.deny-unannotated-endpoints: true` —
  endpoint sem anotação de segurança é negado.
- **Validação de entrada** (Bean Validation) em todos os DTOs; CPF/CNPJ e placa validados com
  regra de negócio (dígitos verificadores / formato Mercosul).
- **Senhas** dos usuários seed nunca hardcoded: se ausentes, são geradas aleatoriamente no startup.
- **Segredos externalizados** por variáveis de ambiente / secrets de CI.
- **Logs** sem exposição de dados sensíveis; tentativas de login inválidas registradas sem credenciais.

---

## 5. Conclusão

As varreduras automatizadas (SCA, SAST, secrets e Sonar) **não acusaram vulnerabilidades
bloqueantes**, e o relatório do OWASP Dependency-Check anexo confirma **ausência de CVEs** nas
dependências. O **R-01** (controle de acesso do canal público da OS), principal ponto de atenção da
revisão manual, foi tratado com a troca do acesso por id por links assinados; resta o **R-02**,
mitigado por configuração. Os artefatos de scan são publicados como
artefatos do pipeline de CI e o relatório de dependências acompanha o repositório
(`dependency-check-report.zip`).
