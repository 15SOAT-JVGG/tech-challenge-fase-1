resource "aws_eks_cluster" "main" {
  name     = var.project_name
  version  = var.kubernetes_version
  role_arn = local.eks_cluster_role_arn

  vpc_config {
    subnet_ids = local.node_subnet_ids

    # O acesso público ao endpoint é o que permite ao operador e à pipeline falarem
    # com o control plane sem bastion; o privado é o caminho dos nós.
    endpoint_public_access  = true
    endpoint_private_access = true
  }

  # Sem isto o acesso ao cluster dependeria de editar o aws-auth ConfigMap à mão, e a
  # sessão do lab ficaria de fora do próprio cluster que acabou de criar. Com o modo
  # API, quem cria vira admin, e o kubeconfig sai de `aws eks update-kubeconfig`.
  access_config {
    authentication_mode                         = "API"
    bootstrap_cluster_creator_admin_permissions = true
  }

  lifecycle {
    # As duas precondições valem meia hora cada: sem elas a falha só apareceria depois
    # da criação do control plane começar.
    precondition {
      condition     = local.eks_cluster_role_arn != null
      error_message = "Esperada exatamente uma role *LabEksClusterRole* na conta. Rode infra/scripts/verify-lab.sh."
    }

    precondition {
      condition     = length(local.node_subnet_ids) >= 2
      error_message = "O EKS exige subnets em ao menos duas AZs que ofertem ${var.node_instance_type}."
    }
  }
}

resource "aws_eks_node_group" "main" {
  cluster_name    = aws_eks_cluster.main.name
  node_group_name = "${var.project_name}-nodes"
  node_role_arn   = local.eks_node_role_arn
  subnet_ids      = local.node_subnet_ids

  instance_types = [var.node_instance_type]
  ami_type       = "AL2023_x86_64_STANDARD"
  capacity_type  = "ON_DEMAND"
  disk_size      = var.node_disk_size

  scaling_config {
    desired_size = var.node_desired_size
    min_size     = var.node_min_size
    max_size     = var.node_max_size
  }

  update_config {
    max_unavailable = 1
  }

  lifecycle {
    precondition {
      condition     = local.eks_node_role_arn != null
      error_message = "Esperada exatamente uma role *LabEksNodeRole* na conta. Rode infra/scripts/verify-lab.sh."
    }
  }
}
