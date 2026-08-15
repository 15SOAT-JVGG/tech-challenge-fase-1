# Kubernetes no Amazon EKS

Os manifests implantam a API e um PostgreSQL 16 no namespace
`oficina-mecanica`. A API começa com duas réplicas e o HPA escala entre duas e
quatro quando CPU ou memória ultrapassa 70% dos respectivos `requests`.

O PostgreSQL roda como um StatefulSet de uma réplica porque o laboratório nega
explicitamente `rds:CreateDBInstance`. A aplicação acessa o banco pelo DNS
interno `postgres.oficina-mecanica.svc.cluster.local`.

## Recursos

| Arquivo | Recurso |
|---|---|
| `namespace.yaml` | Namespace `oficina-mecanica` |
| `configmap.yaml` | Configurações não sensíveis e hostname do PostgreSQL |
| `postgres.yaml` | PostgreSQL StatefulSet, Service headless, PV e PVC de 5 GiB |
| `deployment.yaml` | API Quarkus, probes, recursos e montagem dos secrets |
| `service.yaml` | Service público `LoadBalancer` da API nas portas 8080 e 8090 |
| `hpa.yaml` | Escala horizontal da API entre 2 e 4 pods |

O Secret `oficina-mecanica-secrets` é criado pela pipeline. Nenhum Secret com
valores reais deve ser commitado no repositório.

## Persistência no laboratório

O cluster não possui o driver EBS CSI e o objeto EFS CSI existente não tem pods
do driver em execução. Por isso, `postgres.yaml` usa um PersistentVolume estático
`hostPath` em `/var/lib/oficina-mecanica/postgres`.

Esse volume sobrevive a reinícios do pod no mesmo node, mas os dados são perdidos
se o node Spot for substituído. É uma configuração exclusiva para laboratório.
Em produção, use RDS/Aurora ou instale o EBS CSI com uma role IAM apropriada.

O reclaim policy do PV é `Retain`; excluir o StatefulSet ou PVC não apaga
automaticamente os arquivos no node.

## Cofre do GitHub

Crie um GitHub Environment chamado `aws` em **Settings > Environments** e
cadastre estes Environment secrets:

| Nome | Valor esperado |
|---|---|
| `POSTGRES_USERNAME` | Usuário do PostgreSQL, por exemplo `oficinaadmin` |
| `POSTGRES_PASSWORD` | Senha forte do PostgreSQL |
| `JWT_PRIVATE_KEY` | Conteúdo PEM completo da chave privada RSA |
| `JWT_PUBLIC_KEY` | Conteúdo PEM completo da chave pública RSA |
| `GHCR_PULL_TOKEN` | PAT classic durável com `read:packages` para os pods baixarem a imagem privada |
| `AWS_ACCESS_KEY_ID` | Somente no lab sem OIDC: access key temporária do Vocareum |
| `AWS_SECRET_ACCESS_KEY` | Somente no lab sem OIDC: secret key temporária do Vocareum |
| `AWS_SESSION_TOKEN` | Somente no lab sem OIDC: session token temporário do Vocareum |

Cadastre estas Environment variables:

| Nome | Valor esperado |
|---|---|
| `AWS_ROLE_ARN` | IAM Role assumida por OIDC pelos workflows |
| `AWS_REGION` | `us-east-1` |
| `TF_STATE_BUCKET` | Bucket S3 do state Terraform |
| `TF_STATE_KEY` | `tech-challenge-fase-1/dev/terraform.tfstate` |
| `EKS_CLUSTER_ROLE_ARN` | Role existente confiada ao serviço EKS |
| `EKS_NODE_ROLE_ARN` | Role existente confiada ao EC2 para os nodes EKS |
| `GHCR_USERNAME` | Usuário dono do `GHCR_PULL_TOKEN`; por padrão usa o owner do repositório |

O arquivo local ignorado `postgres.env.local` contém apenas as credenciais para
testes locais. Copie seus valores para `POSTGRES_USERNAME` e
`POSTGRES_PASSWORD` no GitHub Environment.

## Chaves JWT

```shell
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out privateKey.pem
openssl rsa -pubout -in privateKey.pem -out publicKey.pem
gh secret set JWT_PRIVATE_KEY --env aws < privateKey.pem
gh secret set JWT_PUBLIC_KEY --env aws < publicKey.pem
```

## Pipeline completa

O workflow `CI/CD - Build, infrastructure and deploy` executa:

1. quality gate Maven com Spotless, Checkstyle, PMD/CPD, SpotBugs, testes unitários/integrados e JaCoCo mínimo de 80%;
2. geração da versão e publicação da imagem no GHCR com versão, SHA e `latest`;
3. scan bloqueante da imagem com Trivy para vulnerabilidades altas/críticas e secrets;
4. aplicação do Terraform da VPC e EKS;
5. criação do ConfigMap e do Secret Kubernetes;
6. aplicação e espera do rollout do PostgreSQL StatefulSet;
7. aplicação da API, Service e HPA e espera do rollout da API;
8. smoke test do endpoint público;
9. criação da Git tag e GitHub Release;
10. retenção das três imagens mais recentes no GHCR.

Os resultados JUnit e o resumo JaCoCo aparecem na execução da Action. Os
relatórios completos de cobertura e análise estática ficam disponíveis como o
artefato `quality-reports-<sha>` por 14 dias; o resultado do scan da imagem fica
no artefato `trivy-<sha>` pelo mesmo período.

Em execução manual, informe `version` como `vX.Y.Z` ou deixe em branco para usar
`v1.0.<run_number>`. A mesma versão só pode ser repetida para o mesmo commit.

Para a limpeza funcionar com o `GITHUB_TOKEN`, dê ao repositório acesso
**Admin** ao pacote em **Package settings > Manage Actions access**. O token de
pull precisa ser durável porque o `GITHUB_TOKEN` da execução expira e não serve
para futuros restarts ou novos pods do cluster.

O Flyway aplica as migrations quando a API conecta ao PostgreSQL pela primeira
vez.

## Acesso público no laboratório

O Service da API usa `type: LoadBalancer`. No EKS do laboratório, o service
controller cria um Classic Load Balancer público TCP encaminhando a porta 8080
para a API e a porta 8090 para OpenAPI, Swagger UI e os endpoints de
gerenciamento do Quarkus.

Obtenha o hostname público com:

```shell
kubectl get service oficina-mecanica-api \
  --namespace oficina-mecanica \
  --output jsonpath='{.status.loadBalancer.ingress[0].hostname}{"\n"}'
```

A API fica disponível em `http://<hostname>:8080`. A especificação OpenAPI fica
em `http://<hostname>:8090/q/openapi` e a interface Swagger UI em
`http://<hostname>:8090/q/swagger-ui`.

Esta configuração também torna públicos os demais endpoints de gerenciamento
na porta 8090. Ela não possui HTTPS, domínio customizado, WAF ou recursos de API
Gateway e deve ser usada somente no laboratório. O Load Balancer possui cobrança
enquanto existir.

## Verificação

```shell
aws eks update-kubeconfig --region us-east-1 --name oficina-mecanica-dev
kubectl get statefulset,pod,service,pvc -n oficina-mecanica
kubectl get pv oficina-mecanica-postgres
kubectl rollout status statefulset/postgres -n oficina-mecanica
kubectl rollout status deployment/oficina-mecanica-api -n oficina-mecanica
kubectl top pods -n oficina-mecanica
```

O Metrics Server usado pelo HPA é instalado como add-on pelo Terraform.
