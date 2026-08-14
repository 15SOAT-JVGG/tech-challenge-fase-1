resource "aws_eks_cluster" "this" {
  name     = local.name
  role_arn = var.eks_cluster_role_arn
  version  = var.kubernetes_version

  bootstrap_self_managed_addons = false
  enabled_cluster_log_types     = []
  deletion_protection           = false

  access_config {
    authentication_mode                         = "API_AND_CONFIG_MAP"
    bootstrap_cluster_creator_admin_permissions = true
  }

  upgrade_policy {
    support_type = "STANDARD"
  }

  vpc_config {
    subnet_ids              = module.vpc.public_subnets
    endpoint_public_access  = true
    endpoint_private_access = true
  }

  tags = local.common_tags
}

resource "aws_eks_addon" "vpc_cni" {
  cluster_name = aws_eks_cluster.this.name
  addon_name   = "vpc-cni"

  resolve_conflicts_on_create = "OVERWRITE"
  resolve_conflicts_on_update = "OVERWRITE"
}

resource "aws_eks_node_group" "application" {
  cluster_name    = aws_eks_cluster.this.name
  node_group_name = "${local.name}-app"
  node_role_arn   = var.eks_node_role_arn
  subnet_ids      = module.vpc.public_subnets

  ami_type       = "AL2023_x86_64_STANDARD"
  capacity_type  = "SPOT"
  instance_types = var.node_instance_types
  disk_size      = 20

  scaling_config {
    min_size     = var.node_min_size
    max_size     = var.node_max_size
    desired_size = var.node_desired_size
  }

  update_config {
    max_unavailable = 1
  }

  node_repair_config {
    enabled = true
  }

  labels = {
    workload = "application"
  }

  tags = local.common_tags

  depends_on = [aws_eks_addon.vpc_cni]
}

resource "aws_eks_addon" "after_compute" {
  for_each = toset(["coredns", "kube-proxy", "metrics-server"])

  cluster_name = aws_eks_cluster.this.name
  addon_name   = each.value

  resolve_conflicts_on_create = "OVERWRITE"
  resolve_conflicts_on_update = "OVERWRITE"

  depends_on = [aws_eks_node_group.application]
}
