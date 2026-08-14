# Infraestrutura AWS com Terraform

Este Terraform cria uma infraestrutura econômica para demonstração:

- VPC em duas zonas de disponibilidade de `us-east-1`.
- Subnets públicas para os workers EKS, sem NAT Gateway.
- EKS 1.36 com Metrics Server e um managed node group Spot.
- Roles IAM preexistentes para o cluster e os nodes, compatível com labs que bloqueiam a criação de IAM.
- Um worker inicial `t3.medium` ou `t3a.medium`, com capacidade configurada até dois.

O endpoint público do EKS permite que runners hospedados pelo GitHub executem o
deploy. O acesso ainda exige autenticação IAM e uma entrada de acesso do EKS.
Os workers possuem IP público somente para tráfego de saída; seus security groups
não liberam entrada da internet. O PostgreSQL do laboratório é implantado pelos
manifests Kubernetes, não pelo Terraform.

## Custo

Este perfil elimina o NAT Gateway e o banco gerenciado, mas não é gratuito. O
control plane EKS em suporte padrão custa aproximadamente US$ 0,10/h, além de EC2
Spot, IPv4 público, armazenamento e tráfego. Destrua o ambiente quando
não estiver em uso prolongado.

## Bootstrap único

O diretório `bootstrap/` cria:

- Bucket S3 privado, criptografado e versionado para o state.
- Provider OIDC do GitHub na conta AWS.
- IAM Role confiada exclusivamente ao repositório e ao GitHub Environment `aws`.

O bootstrap precisa ser executado uma vez com uma identidade AWS administrativa
local. Ele usa state local; preserve esse state ou importe os recursos antes de
executá-lo novamente.

```shell
cd infra/terraform/bootstrap
cp terraform.tfvars.example terraform.tfvars
# Defina um nome de bucket S3 globalmente único.
terraform init
terraform plan
terraform apply
terraform output
```

Se a conta já tiver o provider OIDC `token.actions.githubusercontent.com`, importe-o
para o bootstrap ou gerencie a IAM Role fora deste código para evitar duplicidade.

O bootstrap anexa `AdministratorAccess` à role para viabilizar EKS, VPC, RDS e as
roles auxiliares numa conta acadêmica isolada. Não reutilize essa política numa
conta compartilhada; substitua-a por uma policy organizacional restrita.

## GitHub Environment `aws`

Crie o Environment `aws` em **Settings > Environments** e configure required
reviewers para os workflows de infraestrutura e deploy.

### Variables

| Nome | Valor |
|---|---|
| `AWS_ROLE_ARN` | Output `github_actions_role_arn` do bootstrap |
| `AWS_REGION` | `us-east-1` |
| `TF_STATE_BUCKET` | Output `state_bucket_name` do bootstrap |
| `TF_STATE_KEY` | `tech-challenge-fase-1/dev/terraform.tfstate` |
| `EKS_CLUSTER_ROLE_ARN` | ARN de uma role existente confiada ao serviço EKS |
| `EKS_NODE_ROLE_ARN` | ARN de uma role existente confiada ao EC2 e preparada para workers EKS |
| `GHCR_USERNAME` | Usuário dono do token usado pelos pods para baixar imagens privadas |

### Secrets

| Nome | Uso |
|---|---|
| `POSTGRES_USERNAME` | Usuário do PostgreSQL StatefulSet; por exemplo `oficinaadmin` |
| `POSTGRES_PASSWORD` | Senha forte do PostgreSQL StatefulSet |
| `JWT_PRIVATE_KEY` | Chave privada PEM montada no pod |
| `JWT_PUBLIC_KEY` | Chave pública PEM montada no pod |
| `GHCR_PULL_TOKEN` | PAT classic durável com `read:packages` para o image pull do EKS |
| `AWS_ACCESS_KEY_ID` | Somente no lab sem OIDC: access key temporária do Vocareum |
| `AWS_SECRET_ACCESS_KEY` | Somente no lab sem OIDC: secret key temporária do Vocareum |
| `AWS_SESSION_TOKEN` | Somente no lab sem OIDC: session token temporário do Vocareum |

Prefira OIDC e não armazene kubeconfig no GitHub. Quando `AWS_ROLE_ARN` estiver
definida, os workflows usam OIDC. Quando ela estiver vazia, usam as três
credenciais temporárias do lab acima e geram o kubeconfig EKS durante a execução.
As credenciais do Vocareum expiram e precisam ser atualizadas nos GitHub Secrets
a cada nova sessão do laboratório.

## Terraform e CI/CD pela pipeline

O workflow **CI/CD - Build, infrastructure and deploy** roda em todo push na
`main` e também pode ser iniciado manualmente com uma versão `vX.Y.Z` opcional.
Antes de criar recursos AWS, ele exige Spotless, Checkstyle, PMD/CPD, SpotBugs,
testes unitários/integrados e cobertura JaCoCo mínima de 80%. Depois desse gate
e da publicação da imagem, ele:

1. gera e aplica o plano Terraform;
2. cria ou atualiza a VPC e o EKS;
3. cria o ConfigMap e o Secret no cluster;
4. aplica o PostgreSQL StatefulSet e aguarda seu rollout;
5. aplica a API, o Service e o HPA.

Após validar a aplicação pública, a pipeline cria a Git tag e a GitHub Release
com uma Action dedicada e mantém somente as três imagens mais recentes no GHCR.

O Service `LoadBalancer` é um recurso Kubernetes. Ao ser aplicado, o service
controller do EKS provisiona o balanceador AWS automaticamente; ele não deve ser
duplicado como um recurso `aws_elb` no Terraform.

O bootstrap continua sendo uma operação local única, pois cria o próprio bucket
de state e a IAM Role OIDC necessária para a pipeline se autenticar.

Para executar somente a infraestrutura, use **Actions > Terraform AWS
infrastructure > Run workflow** e selecione:

- `plan`: valida e mostra as mudanças sem alterar a AWS.
- `apply`: gera um plano novo e o aplica no mesmo job.
- `destroy`: exige que `confirm_destroy` seja exatamente `DESTROY`, exclui
  primeiro o Service `LoadBalancer` e aguarda sua finalização, depois gera e
  aplica um plano Terraform de destruição.

A limpeza do Service precisa acontecer enquanto o cluster ainda está ativo. Se
o EKS for destruído antes, o balanceador criado pelo Kubernetes pode ficar órfão
e continuar gerando cobrança.

O `destroy` remove apenas os recursos do Terraform principal. O bucket S3 de
state/bootstrap é preservado intencionalmente, e o repositório ECR criado
manualmente no laboratório também não é removido. Avalie esses dois recursos
separadamente depois que o destroy terminar e o state não for mais necessário.

O backend S3 usa lockfile nativo (`use_lockfile = true`) e versionamento do bucket.
O histórico versionado do state pode conter o antigo password do Aurora criado
durante os testes; o bucket deve permanecer privado e acessível somente a
administradores e à role do GitHub Actions.

## Operação local do Terraform principal

O script `run-local.sh` executa o fluxo sem armazenar secrets. Use um perfil AWS
SSO e faça login antes:

Em labs Vocareum sem permissão para criar IAM, copie os ARNs das roles EKS
fornecidas pelo laboratório para um arquivo ignorado, por exemplo
`lab.auto.tfvars`:

```hcl
eks_cluster_role_arn = "arn:aws:iam::123456789012:role/LabEksClusterRole"
eks_node_role_arn    = "arn:aws:iam::123456789012:role/LabEksNodeRole"
```

Não execute o bootstrap IAM nesse cenário. Use um bucket S3 que pertença à conta
do lab e informe-o por `TF_STATE_BUCKET`.

```shell
aws sso login --profile oficina-admin
export AWS_PROFILE=oficina-admin

# Executado uma única vez para criar o backend e a role OIDC:
./infra/terraform/run-local.sh bootstrap

# Revisar as mudanças sem alterar a AWS:
./infra/terraform/run-local.sh plan

# Gerar um plano, confirmar e aplicar:
./infra/terraform/run-local.sh apply
```

O script tenta obter o bucket diretamente do output local do bootstrap. Caso o
bootstrap tenha sido executado em outra máquina, informe-o somente para a sessão:

```shell
export TF_STATE_BUCKET='SEU_BUCKET'
./infra/terraform/run-local.sh plan
```

As credenciais do PostgreSQL não são entradas do Terraform. Para testes locais,
elas ficam em `k8s/postgres.env.local`, que é ignorado pelo Git. Não versione
arquivos locais de secrets, states ou planos.
