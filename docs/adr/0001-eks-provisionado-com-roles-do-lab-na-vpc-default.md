# EKS provisionado com recursos Terraform diretos, reusando as roles do lab e a VPC default

O cluster Kubernetes e o RDS da Fase 2 rodam numa conta AWS Academy (Learner Lab), que não concede
`iam:CreateRole`. Por isso o Terraform em `infra/terraform` declara os recursos diretamente
(`aws_eks_cluster`, `aws_eks_node_group`, `aws_db_instance`) reusando roles pré-existentes e a VPC
default, em vez de usar o módulo `terraform-aws-modules/eks`.

## Considered Options

O módulo oficial da comunidade seria o caminho normal, mas ele cria roles IAM para o cluster e para
o node group por conta própria e falha no meio do `apply` nesta conta. Contorná-lo com
`create_iam_role = false` é possível, porém transforma qualquer erro em depuração de código de
terceiros — inaceitável no prazo desta fase. Escrever os recursos à mão também atende melhor o
requisito de documentar quais recursos são criados, já que a lista é exatamente o que está no HCL.

A verificação da conta feita na issue #28 corrigiu a premissa de que a `LabRole` seria a única role
utilizável. O lab também provisiona duas roles dedicadas a EKS, e são elas que o Terraform usa:

| Papel | Role | Por quê |
|---|---|---|
| Cluster | `*-LabEksClusterRole-*` | Tem `AmazonEKSNetworkingPolicy` e `AmazonEKSLoadBalancingPolicy`, que a `LabRole` não tem — a segunda é o que permite ao `Service type=LoadBalancer` criar o ELB. |
| Nós | `*-LabEksNodeRole-*` | Tem `AmazonEKS_CNI_Policy`, ausente na `LabRole`. Sem ela o VPC CNI não atribui IP aos pods e os nós não chegam a `Ready`. |

Manter a `LabRole` nos dois papéis era a decisão original e foi abandonada por causa da CNI: a
`LabRole` confia em `eks.amazonaws.com` e em `ec2.amazonaws.com` e passa na autorização, então o
`apply` teria sucesso e o cluster subiria com nós que nunca ficam prontos — a falha mais cara de
diagnosticar dentro do prazo.

## Consequences

O `/infra` não provisiona rede: se a VPC default for removida da conta, o `apply` quebra.

Os nomes das duas roles de EKS carregam um prefixo gerado pela stack do lab
(`c220532a5561746l...`), que muda a cada reprovisionamento do ambiente. O Terraform precisa
resolvê-las por `data "aws_iam_roles"` com `name_regex` em `LabEksClusterRole` e `LabEksNodeRole`;
um ARN fixo no HCL quebra na próxima turma ou no próximo reset do lab.

O node group não pode receber todas as subnets da VPC default. `us-east-1e` não oferece `t3.medium`,
então incluir a subnet daquela AZ faz a criação do node group falhar. A lista de subnets é filtrada
pelas AZs que ofertam o tipo de instância escolhido, o que o
`data "aws_ec2_instance_type_offerings"` resolve no próprio HCL.

O RDS é liberado por classe de instância: `db.t3.micro`, `db.t4g.micro`, `db.t3.small` e
`db.t3.medium` são permitidos, `db.t3.large` e acima recebem `AccessDenied`. A classe fica como
variável com default em `db.t3.micro`, e subir dela para cima é uma mudança que precisa ser
verificada contra a conta.

Não há separação de privilégio entre control plane e nós além do que essas roles já trazem, e nenhuma
delas é gerenciada por nós — aceitável para um ambiente de avaliação, não para produção.
