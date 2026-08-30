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
}

output "eks_cluster_role_arn" {
  description = "Role do lab assumida pelo control plane do EKS."
  value       = local.eks_cluster_role_arn
}

output "eks_node_role_arn" {
  description = "Role do lab assumida pelos nós do EKS."
  value       = local.eks_node_role_arn
}

output "eks_cluster_name" {
  description = "Nome do cluster, usado no kubeconfig e nos comandos de kubectl."
  value       = aws_eks_cluster.main.name
}

output "eks_cluster_endpoint" {
  description = "Endpoint do servidor de API do cluster."
  value       = aws_eks_cluster.main.endpoint
}

output "eks_cluster_security_group_id" {
  description = "Security group que o EKS anexa aos nós; é a origem liberada no banco."
  value       = aws_eks_cluster.main.vpc_config[0].cluster_security_group_id
}

# O acesso ao cluster sai daqui, não de um passo manual no console.
output "kubeconfig_command" {
  description = "Comando que grava o acesso ao cluster no kubeconfig local."
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${aws_eks_cluster.main.name}"
}

output "database_host" {
  description = "Host do Postgres, lido pela aplicação em INFRA_HOST_POSTGRES."
  value       = aws_db_instance.main.address
}

output "database_port" {
  description = "Porta do Postgres."
  value       = aws_db_instance.main.port
}

output "database_name" {
  description = "Banco criado na instância."
  value       = aws_db_instance.main.db_name
}

output "database_username" {
  description = "Usuário master do banco."
  value       = aws_db_instance.main.username
}

output "database_password" {
  description = "Senha gerada pelo Terraform. Leia com `terraform output -raw database_password`."
  value       = random_password.database.result
  sensitive   = true
}
