# Infraestrutura

Infraestrutura como Código da Fase 2: um cluster EKS, um Postgres gerenciado no RDS e o registry de
container onde a imagem da aplicação é publicada — tudo criado por um único `terraform apply`.

A conta é do AWS Academy (Learner Lab), e isso molda a solução inteira: não há `iam:CreateRole`, a
rede e as roles vêm prontas, e as credenciais da sessão expiram em cerca de quatro horas. As decisões
que decorrem disso estão em [ADR-0001](../docs/adr/0001-eks-provisionado-com-roles-do-lab-na-vpc-default.md).

```
infra/
├── docker/     Dockerfile da aplicação
├── scripts/    bootstrap do state, verificação do lab, publicação da imagem, chaves, credenciais, acesso da pipeline, smoke test e carga
│   └── lib/    o que smoke test e carga compartilham: ferramentas, credencial e endereço do ambiente
└── terraform/  a IaC propriamente dita
k8s/            os manifestos da aplicação, no diretório que o enunciado exige
```

## Do zero ao ambiente no ar

A sequência inteira, na ordem. Cada passo tem uma seção própria mais abaixo explicando o porquê e o
que pode dar errado; aqui está só o roteiro.

```bash
# 1. Sessão do lab ativa e conferida. Rode de novo a cada reset: nomes de role e
#    IDs de rede mudam junto, e o relatório é o que o Terraform espera encontrar.
infra/scripts/verify-lab.sh infra/scripts/lab-capabilities.md

# 2. Infraestrutura. Só na primeira vez na conta: bootstrap-tf-state.sh antes do init.
terraform -chdir=infra/terraform init
terraform -chdir=infra/terraform apply          # 15 a 20 minutos

# 3. Acesso ao cluster.
$(terraform -chdir=infra/terraform output -raw kubeconfig_command)
kubectl get nodes

# 4. O ConfigMap versionado precisa apontar para o banco que acabou de nascer.
#    Se os dois valores divergirem, ajuste k8s/configmap.yaml antes de seguir.
terraform -chdir=infra/terraform output -raw database_host
rg INFRA_HOST_POSTGRES k8s/configmap.yaml

# 5. Imagem no registry.
infra/scripts/publish-image.sh

# 6. Segredos. generate-jwt-pair.sh só na primeira vez; o par vale para os deploys seguintes.
infra/scripts/generate-jwt-pair.sh
export APP_SEED_ADMIN_PASSWORD=... APP_SEED_MECHANIC_PASSWORD=...
infra/scripts/create-app-credentials.sh

# 7. Aplicação. O restart é o que busca a imagem nova, porque a tag é `latest`.
kubectl apply -k k8s/
kubectl rollout restart deploy/oficina-mecanica
kubectl rollout status deploy/oficina-mecanica

# 8. Prova de que funcionou.
infra/scripts/smoke-test.sh
```

Do passo 5 ao 8 é o que a pipeline de entrega passa a fazer a cada merge na `main`. Habilitá-la é um
passo extra, uma vez por cluster — está na seção "Implantando a aplicação", em "Pelo caminho de
entrega".

**Reiniciar o lab não é a mesma coisa que reconstruir o ambiente.** Um `terraform destroy` leva tudo
junto, e aí o roteiro acima vale inteiro. Já um reset de sessão do Learner Lab só cancela as
credenciais: cluster, banco e imagens continuam de pé, e bastam os passos 1 a 3 — renovar a sessão,
rodar o `apply` (que não terá nada a mudar) e regravar o kubeconfig.

O que muda sozinho nesse reset são os **nós**: o lab derruba as instâncias, o node group cria
substitutas, e os pods sobem de novo nelas. Enquanto o banco não volta, a aplicação reinicia em laço
com `Acquisition timeout while waiting for new connection` no Flyway — é esperado e se resolve
sozinho. Espere os pods em `1/1` e confirme com o smoke test antes de concluir que algo quebrou.

## Recursos criados

Tudo abaixo nasce e morre com o Terraform em `infra/terraform`.

| Recurso | Tipo | O que é |
|---|---|---|
| `oficina-mecanica` | `aws_ecr_repository` | Registry da imagem da aplicação, com scan on push |
| — | `aws_ecr_lifecycle_policy` | Expira imagens além das 10 mais recentes |
| `oficina-mecanica` | `aws_eks_cluster` | Control plane do Kubernetes 1.33, endpoint público e privado |
| `oficina-mecanica-nodes` | `aws_eks_node_group` | Nós gerenciados `t3.medium` (AL2023), de 2 a 4 instâncias |
| `metrics-server` | `aws_eks_addon` | Fonte de métricas de recurso do cluster, sem a qual o HPA não escala |
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

### Da máquina do operador

Pré-requisitos: `terraform >= 1.10`, `aws` CLI e uma sessão ativa do lab (`AWS_ACCESS_KEY_ID`,
`AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`).

```bash
# Uma vez por conta: cria o bucket de state. Em outra conta, ajuste o nome do bucket
# no bloco backend de infra/terraform/versions.tf antes de rodar.
infra/scripts/bootstrap-tf-state.sh

# Confere o que a conta autoriza. Rode de novo depois de cada reset do lab:
# IDs de rede e nomes de role mudam junto. O relatório é escrito no caminho passado.
infra/scripts/verify-lab.sh infra/scripts/lab-capabilities.md

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

Um cuidado: recursos que o Kubernetes cria na AWS por conta própria não estão no state. Se houver um
`Service` do tipo `LoadBalancer` no cluster, apague-o com `kubectl` antes do `destroy`, ou o ELB fica
órfão na conta.

### Pela pipeline

O mesmo Terraform e o mesmo state, rodando no GitHub Actions: o workflow **Workflow Infra**
(`.github/workflows/infra.yml`). O disparo é manual, e só manual — nenhum merge começa vinte minutos
de convergência de control plane por acidente (ADR-0003).

O disparo pede a ação:

| Ação | O que faz |
|---|---|
| `plan` | Mostra o que mudaria e para aí. É o default |
| `apply` | Exibe o plano no resumo da execução e aplica exatamente aquele plano |

O plano vai para arquivo justamente para que o `apply` não replaneje: o que roda é o que foi exibido.
Esse arquivo **não** é publicado como artefato — ele carrega a senha do banco em claro. O que vai para
o resumo da execução é o `terraform show` do plano, que redige valor sensível, e ao fim de um `apply`
as saídas do Terraform, onde a senha aparece como `<sensitive>`.

As credenciais são as da sessão do lab, guardadas como secrets do repositório e regravadas a cada
reset: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` e `AWS_SESSION_TOKEN`, todas do painel *AWS
Details* do Learner Lab. O primeiro passo do job é um `aws sts get-caller-identity`, para que uma
sessão vencida falhe em segundos em vez de no meio do apply.

`destroy` não está no workflow: é operação de fim de sessão, e o cuidado com o ELB órfão acima pede
`kubectl` antes do comando — nada disso cabe num clique. Destrua da sua máquina.

### Validação no pull request

Num pull request para a `main`, o `ci.yml` verifica, sem tocar em nuvem:

| Verificação | Comando |
|---|---|
| Formatação do HCL | `terraform -chdir=infra/terraform fmt -check -recursive -diff` |
| Sintaxe e tipos do HCL | `terraform -chdir=infra/terraform init -backend=false` mais `validate` |
| Kustomization dos manifestos | `kubectl kustomize k8s/` |
| Schema dos manifestos | `kubeconform -strict -kubernetes-version 1.33.4` |

O `-backend=false` é o que mantém a checagem fora da nuvem: sem state remoto, sem credencial. E o
schema é verificado pelo `kubeconform`, não por `kubectl apply --dry-run=client`, porque o dry-run de
cliente precisa falar com um servidor de API para validar campo — sem cluster ele não valida nada. Com
`-strict`, um campo inexistente como `imagePullPolcy` reprova o pull request em vez de ser ignorado
silenciosamente pelo cluster.

`terraform plan` fica de fora: exigiria credencial da AWS e o state remoto, que é exatamente o que
esta etapa não quer tocar. Em troca, o que o pull request não pega é ambiente divergente do HCL.

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

## Implantando a aplicação

Os manifestos vivem em `k8s/`, fora de `infra/`, porque é onde o enunciado da fase os exige.

| Objeto | Arquivo | O que carrega |
|---|---|---|
| `ConfigMap` `oficina-mecanica-config` | `k8s/configmap.yaml` | Host e nome do banco, modo TLS, porta, issuer e expiração do JWT, caminho das chaves, usuários de seed, flag do Swagger |
| `Secret` `oficina-mecanica-env` | criado por script | Usuário e senha do banco, senhas de seed |
| `Secret` `oficina-mecanica-jwt` | criado por script | O par RS256, montado como arquivo em `/etc/jwt` |
| `Deployment` `oficina-mecanica` | `k8s/deployment.yaml` | Probes, requests e limits, container non-root; o número de réplicas é do HPA |
| `Service` `oficina-mecanica` | `k8s/service.yaml` | `LoadBalancer`, que o EKS materializa como ELB clássico |
| `HorizontalPodAutoscaler` `oficina-mecanica` | `k8s/hpa.yaml` | De 1 a 6 réplicas, por CPU e memória |

Os dois `Secret` não estão em `k8s/` de propósito — senha do banco, credenciais de seed e chave
privada não entram no repositório. `infra/scripts/create-app-credentials.sh` os monta a partir do par
RS256 e das credenciais do banco: dos outputs do Terraform quando ele está à mão, ou de
`POSTGRES_USERNAME` e `POSTGRES_PASSWORD` quando não — é assim que a pipeline, que não tem credencial
da AWS, entrega os mesmos valores. Com o Terraform, o script recusa a execução se o `ConfigMap`
apontar para um banco diferente do que ele conhece.

Dois valores nos manifestos dependem da conta e do apply, e precisam bater com o que o Terraform
produziu. Numa conta nova, ajuste os dois antes do primeiro deploy:

| Onde | Chave | De onde vem |
|---|---|---|
| `k8s/configmap.yaml` | `INFRA_HOST_POSTGRES` | `terraform -chdir=infra/terraform output -raw database_host` |
| `k8s/kustomization.yaml` | `images[0].newName` | `terraform -chdir=infra/terraform output -raw ecr_repository_url` |

### Da máquina do operador

```bash
# Uma vez: gera o par RS256 fora do build da imagem.
infra/scripts/generate-jwt-pair.sh

export APP_SEED_ADMIN_PASSWORD=... APP_SEED_MECHANIC_PASSWORD=...
infra/scripts/create-app-credentials.sh

infra/scripts/publish-image.sh
kubectl apply -k k8s/
# A tag versionada é `latest`, então o apply sozinho não muda o pod: é o restart que
# faz o Kubernetes buscar a imagem recém-publicada.
kubectl rollout restart deploy/oficina-mecanica
kubectl rollout status deploy/oficina-mecanica
```

O endereço público sai do `Service`; o ELB leva cerca de um minuto para começar a responder:

```bash
ELB="$(kubectl get svc oficina-mecanica -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')"
curl "http://$ELB/q/health/ready"
```

### Pelo caminho de entrega

O mesmo deploy acima, sem nenhum passo manual: o workflow **Workflow Deploy**
(`.github/workflows/deploy.yml`) roda a cada merge na `main` e leva a aplicação ao cluster. São três
jobs encadeados, e a etapa que falha interrompe a entrega — teste vermelho não gera imagem, e imagem
que não publicou não chega ao cluster.

| Job | O que faz | Credencial |
|---|---|---|
| Build & Tests | `./mvnw verify`: build, testes, qualidade estática e o gate de cobertura | nenhuma |
| Imagem no ECR | Publica a imagem com a tag do SHA do commit | sessão do lab |
| Deploy no cluster | Entrega os `Secret`, aplica `k8s/` com a tag do commit, aguarda o rollout e roda o smoke test | token da `ServiceAccount` |

A tag da imagem é o SHA do commit, e é ela que faz do `kubectl apply` um rollout de verdade: com
`latest`, o objeto no cluster não mudaria e nenhum pod seria substituído. Por isso o job troca a tag
com `kustomize edit set image` antes de aplicar, e o caminho manual precisa do `rollout restart` que
ele dispensa.

As migrations não têm passo próprio. `flyway.migrate-at-start` as aplica na inicialização, então quem
as espera é o `kubectl rollout status`: um pod só fica pronto depois de migrar o banco (ADR-0003). E o
último passo é o smoke test — se ele passa, o que subiu está operacional de fora para dentro.

**O acesso ao cluster não usa credencial da AWS** (ADR-0002). A sessão do Learner Lab expira em cerca
de quatro horas, e o deploy ficaria vermelho por motivo alheio ao código. Ele autentica com o token de
uma `ServiceAccount` dedicada, criada uma vez por cluster:

```bash
# Com o kubeconfig de administrador ativo. Cria a ServiceAccount github-deployer, uma
# Role com só o que o deploy faz, e grava o kubeconfig em .local-kube/.
infra/scripts/create-deploy-kubeconfig.sh
```

O comando imprime, ao fim, o `gh secret set` que guarda esse kubeconfig no repositório. O token é de
longa duração e concede acesso ao cluster: se vazar, não expira sozinho, e a mitigação é
`kubectl delete serviceaccount github-deployer` e gerar outro.

Os secrets do repositório que a entrega consome:

| Secret | De onde vem |
|---|---|
| `KUBE_CONFIG_B64` | `infra/scripts/create-deploy-kubeconfig.sh` |
| `POSTGRES_USERNAME` / `POSTGRES_PASSWORD` | `terraform -chdir=infra/terraform output -raw database_username` / `database_password` |
| `APP_SEED_ADMIN_PASSWORD` / `APP_SEED_MECHANIC_PASSWORD` | escolhidas pelo operador; são as credenciais de acesso à API |
| `JWT_PRIVATE_KEY_B64` / `JWT_PUBLIC_KEY_B64` | `infra/scripts/generate-jwt-pair.sh` |
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` | painel *AWS Details* do lab; só o job da imagem os usa |

Nenhum deles aparece em log: o GitHub redige valor de secret na saída, e nem o workflow nem os
scripts imprimem credencial — `create-app-credentials.sh` monta os `Secret` por
`kubectl create ... --dry-run=client -o yaml | kubectl apply -f -`, sem echo pelo caminho.

Um `apply` do Terraform que recrie o banco troca a senha e o endereço: regrave `POSTGRES_PASSWORD` e
atualize `INFRA_HOST_POSTGRES` no `ConfigMap` antes do próximo merge, ou o pod novo não fica pronto.

Se o rollout ou o smoke test falharem, o job colhe `kubectl get pods`, `describe` do `Deployment` e as
últimas cem linhas de log da aplicação — quase sempre é o suficiente para saber o motivo sem abrir o
`kubectl`.

## Smoke test

Um comando responde se o deploy funcionou. Ele descobre o endereço do `Service` sozinho — esperando o
ELB nascer, se for o caso —, espera o readiness, autentica com o usuário de seed, abre uma ordem de serviço e a recupera na listagem — o que
exercita de uma vez o ELB, o pod, o `ConfigMap`, os dois `Secret`, a conectividade com o RDS e as
migrations. Sai com código diferente de zero na primeira verificação que falhar.

```bash
export APP_SEED_ADMIN_PASSWORD=...       # a mesma senha entregue no Secret
infra/scripts/smoke-test.sh              # ou: infra/scripts/smoke-test.sh http://localhost:8080
```

Ele é também o último passo da pipeline de entrega e o roteiro da parte de consumo das APIs na
demonstração. Regra de negócio ele não verifica — isso é o que os `*IT` da aplicação já fazem contra um
Postgres real, e por isso uma falha aqui aponta para infraestrutura.

Requer `curl` e `jq`, mais `kubectl` quando o endereço não é informado. O cliente, o veículo e a OS
criados ficam no banco depois da execução — são os dados que a demonstração usa em seguida.

Duas coisas que só aparecem contra o RDS e valem lembrar:

- **TLS é obrigatório.** O Postgres 16 no RDS recusa conexão em claro, então o `ConfigMap` manda
  `POSTGRES_SSLMODE=require`. O default da aplicação é `disable`, que é o que o Postgres do
  docker compose fala.
- **As migrations sobem sozinhas.** `V1__create_schema.sql` cria o schema `oficina_mecanica` e o
  Flyway roda na inicialização, então o primeiro deploy converge contra um banco vazio. Uma réplica
  no primeiro rollout é o que evita duas instâncias migrando ao mesmo tempo.

## Escalabilidade automática

A aplicação ganha réplicas quando a carga sobe e as devolve quando a carga cai, sem intervenção. Quem
decide é o `HorizontalPodAutoscaler` de `k8s/hpa.yaml`, que compara o consumo dos pods com os
`requests` declarados no `Deployment` — daí a exigência de declará-los.

| Ajuste | Valor | Por quê |
|---|---|---|
| Réplicas | de 1 a 6 | Uma no vale deixa o Flyway migrar sem concorrência; seis cabem nos dois nós `t3.medium` sem depender de nós novos |
| CPU | 60% do request de 250m | É a métrica que manda na prática: a verificação BCrypt do login é cara em processador |
| Memória | 80% do request de 512Mi | Rede de segurança para carga que pese no heap; o pod ocioso fica perto de 250Mi, bem abaixo do alvo |
| Subida | imediata, até 2 pods a cada 15s | O default já é imediato; o passo maior encurta a demonstração |
| Descida | 1 pod a cada 30s, após 1 min de calmaria | O default espera 5 minutos, o que não cabe num vídeo de 15 |

A fonte das métricas é o `metrics-server`, instalado pelo Terraform como addon gerenciado do EKS. Um
cluster nasce sem ele, e sem ele o HPA lê `<unknown>` e nunca escala:

```bash
kubectl top pods    # se isto responder, o HPA tem base de cálculo
```

### Reproduzindo a demonstração

Dois terminais. No primeiro, o observador:

```bash
kubectl get hpa,pods -w
```

No segundo, a carga — uma rajada de logins, o endpoint mais caro em CPU da API:

```bash
export APP_SEED_ADMIN_PASSWORD=...       # a mesma senha entregue no Secret
infra/scripts/load-test.sh               # 40 conexões por 5 minutos
```

Duração e concorrência saem de `LOAD_DURATION_SECONDS` e `LOAD_CONCURRENCY`. Na medição de
referência, `LOAD_CONCURRENCY=30` já levou o pod a 138% do seu request de CPU, e o autoscaler criou
réplicas em menos de um minuto:

```
SuccessfulRescale   New size: 3; reason: cpu resource utilization (percentage of request) above target
SuccessfulRescale   New size: 2; reason: All metrics below target
```

O segundo evento é o do fim da carga: passado o minuto de calmaria, as réplicas caem uma a uma até
voltar a uma. O ciclo inteiro leva cerca de dez minutos.

Aplique os manifestos **antes** de começar a carga, nunca durante. Um `kubectl apply -k k8s/` no meio
da rajada devolve o `Deployment` a uma réplica por um instante — é o efeito de remover o campo
`replicas`, que o apply poda do objeto vivo — e a demonstração dá um solavanco que não é do
autoscaler.

Se o HPA insistir em `<unknown>` **sob carga**, olhe as probes antes do autoscaler. Um pod no teto da
sua cota de CPU demora a responder, e o HPA ignora pod não-pronto: com o timeout default de um
segundo, o único pod saía da conta justamente quando havia carga para medir. É por isso que as três
probes do `Deployment` declaram `timeoutSeconds: 5`.

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
