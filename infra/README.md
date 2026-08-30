# Infraestrutura

Infraestrutura como Código da Fase 2: um cluster EKS, um Postgres gerenciado no RDS e o registry de
container onde a imagem da aplicação é publicada — tudo criado por um único `terraform apply`.

A conta é do AWS Academy (Learner Lab), e isso molda a solução inteira: não há `iam:CreateRole`, a
rede e as roles vêm prontas, e as credenciais da sessão expiram em cerca de quatro horas. As decisões
que decorrem disso estão em [ADR-0001](../docs/adr/0001-eks-provisionado-com-roles-do-lab-na-vpc-default.md).

```
infra/
├── docker/     Dockerfile da aplicação
├── scripts/    bootstrap do state, verificação do lab, publicação da imagem, chaves RS256
└── terraform/  a IaC propriamente dita
```

## Recursos criados

Tudo abaixo nasce e morre com o Terraform em `infra/terraform`.

| Recurso | Tipo | O que é |
|---|---|---|
| `oficina-mecanica` | `aws_ecr_repository` | Registry da imagem da aplicação, com scan on push |
| — | `aws_ecr_lifecycle_policy` | Expira imagens além das 10 mais recentes |
| `oficina-mecanica` | `aws_eks_cluster` | Control plane do Kubernetes 1.33, endpoint público e privado |
| `oficina-mecanica-nodes` | `aws_eks_node_group` | Nós gerenciados `t3.medium` (AL2023), de 2 a 4 instâncias |
| `oficina-mecanica-db` | `aws_db_instance` | Postgres 16 em `db.t3.micro`, disco gp3 criptografado, sem acesso público |
| `oficina-mecanica-db` | `aws_db_subnet_group` | Subnets onde a instância pode nascer |
| `oficina-mecanica-db` | `aws_security_group` | Fecha o banco; a única entrada é a regra abaixo |
| — | `aws_vpc_security_group_ingress_rule` | Libera a porta 5432 só para o security group dos nós do cluster |
| — | `random_password` | Senha do banco, gerada no apply e nunca versionada |

Não são criados pelo Terraform, por escolha ou por restrição da conta:

| Recurso | Origem |
|---|---|
| VPC default, subnets | Já existem na conta; resolvidos por data source |
| `*-LabEksClusterRole-*`, `*-LabEksNodeRole-*` | Provisionadas pela stack do lab; resolvidas por `name_regex` |
| Bucket S3 do state | `infra/scripts/bootstrap-tf-state.sh` — o backend precisa existir antes do primeiro `init` |

O node group ocupa 5 das 6 subnets da VPC default. `us-east-1e` fica de fora porque não oferta
`t3.medium`, e o banco acompanha as mesmas AZs — aquela zona também não oferta `db.t3.micro`.

## Provisionando

Pré-requisitos: `terraform >= 1.10`, `aws` CLI e uma sessão ativa do lab (`AWS_ACCESS_KEY_ID`,
`AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`).

```bash
# Uma vez por conta: cria o bucket de state. Em outra conta, ajuste o nome do bucket
# no bloco backend de infra/terraform/versions.tf antes de rodar.
infra/scripts/bootstrap-tf-state.sh

# Confere o que a conta autoriza e regrava infra/scripts/lab-capabilities.md.
# Rode de novo depois de cada reset do lab: IDs de rede e nomes de role mudam.
infra/scripts/verify-lab.sh

terraform -chdir=infra/terraform init
terraform -chdir=infra/terraform apply
```

O `apply` leva de 15 a 20 minutos: o control plane do EKS e a instância do RDS convergem em paralelo,
e o node group só começa depois que o cluster está pronto.

Para destruir tudo — recomendado ao fim de cada sessão longa, para não queimar crédito:

```bash
terraform -chdir=infra/terraform destroy
```

O `destroy` remove os nove recursos da tabela acima e nada mais — a rede e as roles não são nossas.
O bucket de state sobrevive de propósito; subir o ambiente de novo é um `apply`.

Um cuidado: recursos criados de dentro do Kubernetes não estão no state. Se houver um `Service` do
tipo `LoadBalancer` no cluster, apague-o (`kubectl delete -k k8s/`) antes do `destroy`, ou o ELB fica
órfão na conta.

## Acessando o que foi criado

O acesso ao cluster sai das saídas do Terraform, sem nenhum passo no console:

```bash
$(terraform -chdir=infra/terraform output -raw kubeconfig_command)
kubectl get nodes
```

Os nós aparecem em `Ready` em poucos minutos após o fim do apply.

O endereço e as credenciais do banco saem do mesmo lugar:

```bash
terraform -chdir=infra/terraform output database_host
terraform -chdir=infra/terraform output -raw database_password
```

A instância não é pública: só o security group dos nós do cluster alcança a porta 5432. Para conferir
de fora, use um pod dentro do cluster:

```bash
kubectl run psql --rm -it --image=postgres:16 --restart=Never -- \
  psql "postgresql://oficina:$(terraform -chdir=infra/terraform output -raw database_password)@$(terraform -chdir=infra/terraform output -raw database_host):5432/oficina" -c 'select version()'
```

## Segredos

A senha do banco é gerada por `random_password` no apply e nunca aparece em arquivo versionado. Ela
vive apenas no state remoto, num bucket S3 criptografado e com acesso público bloqueado, e é lida por
`terraform output -raw database_password`.

Pelo mesmo motivo o state **não** pode ser local: `terraform.tfstate` está no `.gitignore`, e o
backend S3 é o que mantém a senha fora do repositório sem sumir entre execuções.

O par de chaves RS256 do JWT segue caminho parecido — gerado uma vez por
`infra/scripts/generate-jwt-pair.sh`, fora do build da imagem, e injetado na aplicação como segredo.

## Publicando a imagem

```bash
infra/scripts/publish-image.sh          # tag = SHA do commit atual
infra/scripts/publish-image.sh 1.0.0    # ou uma tag explícita
```

## Variáveis

Todas têm default e nenhuma é obrigatória. As que mais importam:

| Variável | Default | Por quê |
|---|---|---|
| `aws_region` | `us-east-1` | Região do lab |
| `project_name` | `oficina-mecanica` | Prefixo de nome e tag `Project` de todo recurso |
| `kubernetes_version` | `1.33` | Versão do control plane |
| `node_instance_type` | `t3.medium` | Define quais AZs podem receber node group |
| `node_desired_size` / `node_min_size` / `node_max_size` | `2` / `2` / `4` | Tamanho do node group |
| `database_instance_class` | `db.t3.micro` | A conta nega `db.t3.large` para cima; a validação da variável recusa classes bloqueadas |
| `database_name` / `database_username` | `oficina` / `oficina` | O que a aplicação lê em `POSTGRES_DB` e `POSTGRES_USERNAME` |
