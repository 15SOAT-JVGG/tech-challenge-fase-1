variable "aws_region" {
  description = "Region of the Terraform state bucket."
  type        = string
  default     = "us-east-1"
}

variable "state_bucket_name" {
  description = "Globally unique S3 bucket name for the main Terraform state."
  type        = string
}

variable "github_repository" {
  description = "GitHub owner/repository allowed to assume the deployment role."
  type        = string
  default     = "15SOAT-JVGG/tech-challenge-fase-1"
}

variable "github_environment" {
  description = "GitHub Environment allowed by the OIDC trust policy."
  type        = string
  default     = "aws"
}
