variable "aws_region" {
  description = "Região onde a infraestrutura da fase é provisionada."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Prefixo de nome e tag Project de todo recurso criado."
  type        = string
  default     = "oficina-mecanica"
}

variable "node_instance_type" {
  description = "Tipo de instância dos nós do EKS. Define quais AZs podem receber node group."
  type        = string
  default     = "t3.medium"
}

variable "image_retention_count" {
  description = "Quantidade de imagens mantidas no ECR; as mais antigas são expiradas."
  type        = number
  default     = 10
}
