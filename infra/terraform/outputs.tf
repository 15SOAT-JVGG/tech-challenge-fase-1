output "aws_region" {
  description = "AWS Region containing the infrastructure."
  value       = var.aws_region
}

output "vpc_id" {
  description = "Application VPC ID."
  value       = module.vpc.vpc_id
}

output "eks_cluster_name" {
  description = "EKS cluster name used by the deployment workflow."
  value       = aws_eks_cluster.this.name
}

output "eks_cluster_endpoint" {
  description = "EKS Kubernetes API endpoint."
  value       = aws_eks_cluster.this.endpoint
}
