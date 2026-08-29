# EKS provisionado com recursos Terraform diretos, reusando a LabRole e a VPC default

O cluster Kubernetes e o RDS da Fase 2 rodam numa conta AWS Academy (Learner Lab), que não concede
`iam:CreateRole` e expõe uma única role pré-existente, a `LabRole`. Por isso o Terraform em
`infra/terraform` declara os recursos diretamente (`aws_eks_cluster`, `aws_eks_node_group`,
`aws_db_instance`) apontando `role_arn` para a `LabRole` e usando a VPC default e suas subnets,
em vez de usar o módulo `terraform-aws-modules/eks`.

## Considered Options

O módulo oficial da comunidade seria o caminho normal, mas ele cria roles IAM para o cluster e para
o node group por conta própria e falha no meio do `apply` nesta conta. Contorná-lo com
`create_iam_role = false` é possível, porém transforma qualquer erro em depuração de código de
terceiros — inaceitável no prazo desta fase. Escrever os recursos à mão também atende melhor o
requisito de documentar quais recursos são criados, já que a lista é exatamente o que está no HCL.

## Consequences

O `/infra` não provisiona rede: se a VPC default for removida da conta, o `apply` quebra. A
`LabRole` é compartilhada por todos os recursos do lab, então não há separação de privilégio entre
control plane e nós — aceitável para um ambiente de avaliação, não para produção.
