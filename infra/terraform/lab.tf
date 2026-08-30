# Rede e roles IAM já existem na conta do Learner Lab e não são criadas por esta IaC
# (ADR-0001). Resolvê-las por data source é o que faz o HCL sobreviver a um reset do
# lab: cada reprovisionamento troca os IDs de VPC e subnet e o prefixo do nome das roles.

data "aws_iam_roles" "eks_cluster" {
  name_regex = ".*LabEksClusterRole.*"
}

data "aws_iam_roles" "eks_node" {
  name_regex = ".*LabEksNodeRole.*"
}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

data "aws_subnet" "default" {
  for_each = toset(data.aws_subnets.default.ids)

  id = each.value
}

# A criação do node group falha se receber uma subnet numa AZ que não oferta o tipo de
# instância — us-east-1e não oferta t3.medium.
data "aws_ec2_instance_type_offerings" "node" {
  location_type = "availability-zone"

  filter {
    name   = "instance-type"
    values = [var.node_instance_type]
  }
}

locals {
  # A role de nó é a dedicada do lab, não a LabRole: só ela traz AmazonEKS_CNI_Policy,
  # e sem CNI os nós sobem mas nunca chegam a Ready (ADR-0001).
  #
  # O try existe porque one() aborta com erro cripto quando o regex casa com mais de
  # uma role; virando null, a precondição do output explica o que fazer.
  eks_cluster_role_arn = try(one(data.aws_iam_roles.eks_cluster.arns), null)
  eks_node_role_arn    = try(one(data.aws_iam_roles.eks_node.arns), null)

  node_availability_zones = toset(data.aws_ec2_instance_type_offerings.node.locations)
  node_subnet_ids = [
    for subnet in data.aws_subnet.default : subnet.id
    if contains(local.node_availability_zones, subnet.availability_zone)
  ]
}
