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

variable "kubernetes_version" {
  description = "Versão do control plane do EKS."
  type        = string
  default     = "1.33"
}

variable "node_disk_size" {
  description = "Disco de cada nó, em GB."
  type        = number
  default     = 20
}

variable "node_desired_size" {
  description = "Quantidade de nós com que o node group nasce."
  type        = number
  default     = 2
}

variable "node_min_size" {
  description = "Piso de nós do node group."
  type        = number
  default     = 2
}

variable "node_max_size" {
  description = "Teto de nós do node group."
  type        = number
  default     = 4
}

variable "database_engine_version" {
  description = "Versão do Postgres no RDS. Prefixo de major aceita a menor mais recente disponível."
  type        = string
  default     = "16"
}

# A conta do lab libera rds:CreateDBInstance por classe: até db.t3.medium passa, de
# db.t3.large para cima recebe AccessDenied (ADR-0001).
variable "database_instance_class" {
  description = "Classe da instância do RDS."
  type        = string
  default     = "db.t3.micro"

  validation {
    condition     = contains(["db.t3.micro", "db.t4g.micro", "db.t3.small", "db.t3.medium"], var.database_instance_class)
    error_message = "A conta do Learner Lab só autoriza db.t3.micro, db.t4g.micro, db.t3.small e db.t3.medium."
  }
}

variable "database_allocated_storage" {
  description = "Disco da instância do RDS, em GB."
  type        = number
  default     = 20
}

variable "database_name" {
  description = "Nome do banco criado na instância, lido pela aplicação em POSTGRES_DB."
  type        = string
  default     = "oficina"
}

variable "database_username" {
  description = "Usuário master do banco, lido pela aplicação em POSTGRES_USERNAME."
  type        = string
  default     = "oficina"
}
