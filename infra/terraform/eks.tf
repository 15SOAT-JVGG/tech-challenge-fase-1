locals {
  # O grupo que o EKS cria e anexa aos nós do node group gerenciado. É a origem que o
  # banco libera, e o [0] fica escondido aqui em vez de repetido em cada consumidor.
  cluster_security_group_id = aws_eks_cluster.main.vpc_config[0].cluster_security_group_id
}

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

# Um cluster EKS nasce sem servidor de métricas de recurso, e sem ele o HPA lê
# <unknown> e nunca escala. O addon gerenciado entrega o mesmo metrics-server do
# upstream sem pedir IAM, e — ao contrário de um `kubectl apply` avulso — entra no
# state, então o destroy leva o cluster inteiro junto.
resource "aws_eks_addon" "metrics_server" {
  cluster_name = aws_eks_cluster.main.name
  addon_name   = "metrics-server"

  # addon_version fica de fora de propósito: o default que o EKS publica acompanha a
  # versão do control plane, e fixá-lo aqui obrigaria a revisar o HCL a cada bump de
  # kubernetes_version.
  #
  # OVERWRITE é o que deixa o addon assumir uma instalação anterior feita à mão, em
  # vez de falhar o apply por conflito de propriedade dos objetos.
  resolve_conflicts_on_create = "OVERWRITE"
  resolve_conflicts_on_update = "OVERWRITE"

  # Os pods do addon precisam de nó onde rodar; sem isto o apply os cria em Pending e
  # espera o timeout do addon antes de falhar.
  depends_on = [aws_eks_node_group.main]
}
