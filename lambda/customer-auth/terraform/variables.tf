variable "aws_region" {
  description = "Região da função; a mesma da plataforma."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Prefixo de nome e tag Project, o mesmo da stack da plataforma."
  type        = string
  default     = "oficina-mecanica"
}

variable "platform_state_bucket" {
  description = "Bucket do state da plataforma (infra/terraform), de onde saem rede, banco e API Gateway."
  type        = string
  default     = "oficina-mecanica-tf-state-059056506203"
}

variable "platform_state_key" {
  description = "Chave do state da plataforma no bucket."
  type        = string
  default     = "fase2/terraform.tfstate"
}

variable "package_path" {
  description = "JAR sombreado da função, gerado por `./mvnw package` em lambda/customer-auth."
  type        = string
  default     = "../target/customer-auth.jar"
}

# É o mesmo valor do secret JWT_PRIVATE_KEY_B64 que a pipeline da API monta no pod: a função
# assina com a chave cuja pública a API usa para validar. Chega por TF_VAR_jwt_private_key_b64.
variable "jwt_private_key_b64" {
  description = "Chave privada RS256 em PEM PKCS#8, codificada em base64."
  type        = string
  sensitive   = true
}

variable "jwt_issuer" {
  description = "Issuer do token; precisa ser igual ao JWT_ISSUER da API."
  type        = string
  default     = "oficina-api"
}

variable "token_lifetime_minutes" {
  description = "Validade do token do cliente, em minutos."
  type        = number
  default     = 60
}

variable "memory_size" {
  description = "Memória da função em MB. Na Lambda a CPU cresce junto com a memória, e é ela que encurta o cold start da JVM."
  type        = number
  default     = 1024
}

variable "log_retention_days" {
  description = "Retenção do log group da função, em dias."
  type        = number
  default     = 7
}
