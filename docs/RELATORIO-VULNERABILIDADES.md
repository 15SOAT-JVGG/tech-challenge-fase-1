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

> Defesa em profundidade: secrets → código (SAST) → dependências (SCA).

---

## 2. Resultado das varreduras

### 2.1 SCA — OWASP Dependency-Check
- **Resultado: nenhuma dependência vulnerável.** O relatório gerado
  (`dependency-check-report.zip`, anexo na raiz do repositório) **não apresenta CVEs**
  (0 dependências marcadas como *Vulnerable*).
- Política: o build falha automaticamente para qualquer CVE com **CVSS ≥ 8**.

### 2.2 Secrets — Gitleaks
- Varredura de todo o histórico (`fetch_depth: 0`). Sem segredos commitados.
- O par de chaves RSA versionado em `src/main/resources/jwt` é **exclusivo para desenvolvimento**
  e está documentado como tal; em produção é sobrescrito por secrets montados
  (`JWT_PRIVATE_KEY_LOCATION` / `JWT_PUBLIC_KEY_LOCATION`). Não é um segredo real de produção.

### 2.3 SAST — Semgrep
- Regras `p/ci` com `fail_on_findings: true` — o merge na `main` só ocorre sem achados bloqueantes.

---

## 3. Registro de riscos (análise manual)

Além dos scans automatizados, a revisão arquitetural identificou os riscos abaixo.

### R-01 — Canal público da OS sem verificação de propriedade (Broken Access Control / IDOR)
- **Severidade:** Média · **OWASP Top 10:** A01:2021 (Broken Access Control) · **OWASP ASVS:** V4.2
- **Componente:** `PublicWorkOrderController` (`/v1/public/work-orders/**`, `@PermitAll`).
- **Descrição:** os endpoints públicos de acompanhamento e de **aprovação/rejeição de orçamento**
  autorizam a operação apenas com base no **UUID da OS** informado no path. Não há autenticação
  do cliente nem vínculo verificável entre quem chama e o dono da OS. Quem obtiver/vazar o UUID
  (logs, histórico de navegador, encaminhamento de link) consegue visualizar os dados completos
  da OS e **aprovar ou rejeitar o orçamento** de qualquer ordem.
- **Fator atenuante:** o identificador é um **UUID v4** (não sequencial/enumerável), o que torna
  inviável a adivinhação por força bruta. O UUID funciona, na prática, como um *capability token*.
- **Impacto:** alteração indevida do ciclo de vida da OS (aprovação/rejeição) e exposição de
  dados do cliente/veículo caso o identificador vaze.
- **Recomendação:** adicionar um segredo por OS além do UUID (ex.: token de acompanhamento
  assinado/efêmero enviado ao cliente), ou autenticar o cliente e validar a titularidade da OS
  antes de permitir aprovação/rejeição. Registrar (audit log) as decisões feitas pelo canal público.

### R-02 — Swagger UI exposto no artefato empacotado
- **Severidade:** Baixa · **OWASP Top 10:** A05:2021 (Security Misconfiguration)
- **Descrição:** o Swagger UI fica acessível também no artefato empacotado durante a fase de
  desenvolvimento (`always-include: ${SWAGGER_UI_ENABLED:true}`), expondo a superfície da API.
- **Atenuante/Recomendação:** já previsto — em produção definir `SWAGGER_UI_ENABLED=false`.

---

## 4. Controles de segurança já implementados

- **Autenticação JWT (RS256)** com chaves assimétricas externalizáveis; emissor validado.
- **Autorização RBAC** por endpoint (`@RolesAllowed("ADMIN" | "MECHANIC")`).
- **Negação por padrão:** `quarkus.security.jaxrs.deny-unannotated-endpoints: true` —
  endpoint sem anotação de segurança é negado.
- **Validação de entrada** (Bean Validation) em todos os DTOs; CPF/CNPJ e placa validados com
  regra de negócio (dígitos verificadores / formato Mercosul).
- **Senhas** dos usuários seed nunca hardcoded: se ausentes, são geradas aleatoriamente no startup.
- **Segredos externalizados** por variáveis de ambiente / secrets de CI.
- **Logs** sem exposição de dados sensíveis; tentativas de login inválidas registradas sem credenciais.

---

## 5. Conclusão

As varreduras automatizadas (SCA, SAST e secrets) **não acusaram vulnerabilidades
bloqueantes**, e o relatório do OWASP Dependency-Check anexo confirma **ausência de CVEs** nas
dependências. O principal ponto de atenção é o **R-01** (controle de acesso do canal público da OS),
recomendado para tratamento em evolução do MVP. Os artefatos de scan são publicados como
artefatos do pipeline de CI e o relatório de dependências acompanha o repositório
(`dependency-check-report.zip`).
