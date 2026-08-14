variable "aws_region" {
  description = "AWS Region used by all resources."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Prefix used to name and tag resources."
  type        = string
  default     = "oficina-mecanica"
}

variable "environment" {
  description = "Environment identifier."
  type        = string
  default     = "dev"
}

variable "vpc_cidr" {
  description = "CIDR allocated to the application VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "kubernetes_version" {
  description = "Kubernetes minor version used by EKS."
  type        = string
  default     = "1.36"
}

variable "eks_cluster_role_arn" {
  description = "ARN of an existing IAM role trusted by eks.amazonaws.com."
  type        = string

  validation {
    condition     = can(regex("^arn:[^:]+:iam::[0-9]{12}:role/.+$", var.eks_cluster_role_arn))
    error_message = "eks_cluster_role_arn must be a valid IAM role ARN."
  }
}

variable "eks_node_role_arn" {
  description = "ARN of an existing IAM role trusted by ec2.amazonaws.com for the EKS nodes."
  type        = string

  validation {
    condition     = can(regex("^arn:[^:]+:iam::[0-9]{12}:role/.+$", var.eks_node_role_arn))
    error_message = "eks_node_role_arn must be a valid IAM role ARN."
  }
}

variable "node_instance_types" {
  description = "Diversified EC2 types accepted by the Spot managed node group."
  type        = list(string)
  default     = ["t3.medium", "t3a.medium"]
}

variable "node_min_size" {
  description = "Minimum number of EKS worker nodes."
  type        = number
  default     = 1
}

variable "node_max_size" {
  description = "Maximum number of EKS worker nodes."
  type        = number
  default     = 2
}

variable "node_desired_size" {
  description = "Initial number of EKS worker nodes."
  type        = number
  default     = 1
}
