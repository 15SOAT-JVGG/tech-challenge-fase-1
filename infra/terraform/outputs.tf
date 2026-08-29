output "ecr_registry" {
  description = "Endereço do registry, usado no docker login."
  value       = split("/", aws_ecr_repository.app.repository_url)[0]
}

output "ecr_repository_url" {
  description = "Repositório onde a imagem da aplicação é publicada."
  value       = aws_ecr_repository.app.repository_url
}

output "vpc_id" {
  description = "VPC default da conta, onde cluster e banco são criados."
  value       = data.aws_vpc.default.id
}

output "subnet_ids" {
  description = "Todas as subnets da VPC default."
  value       = sort(data.aws_subnets.default.ids)
}

output "node_subnet_ids" {
  description = "Subnets aptas a receber o node group, filtradas pela oferta do tipo de instância dos nós."
  value       = sort(local.node_subnet_ids)

  precondition {
    # O EKS exige subnets em pelo menos duas AZs; sem isso a falha só apareceria
    # no meio do apply do cluster, minutos depois.
    condition     = length(local.node_subnet_ids) >= 2
    error_message = "A VPC default precisa de subnets em ao menos duas AZs que ofertem ${var.node_instance_type}."
  }
}

output "eks_cluster_role_arn" {
  description = "Role do lab assumida pelo control plane do EKS."
  value       = local.eks_cluster_role_arn

  precondition {
    condition     = local.eks_cluster_role_arn != null
    error_message = "Esperada exatamente uma role *LabEksClusterRole* na conta. Rode infra/scripts/verify-lab.sh."
  }
}

output "eks_node_role_arn" {
  description = "Role do lab assumida pelos nós do EKS."
  value       = local.eks_node_role_arn

  precondition {
    condition     = local.eks_node_role_arn != null
    error_message = "Esperada exatamente uma role *LabEksNodeRole* na conta. Rode infra/scripts/verify-lab.sh."
  }
}
