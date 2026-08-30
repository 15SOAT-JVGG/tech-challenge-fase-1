locals {
  postgres_port = 5432
}

# A senha nasce aqui e nunca entra em arquivo versionado: o valor vive só no state
# remoto (S3, criptografado) e sai por `terraform output -raw database_password`.
#
# Sem caracteres especiais porque a senha viaja dentro de uma URL de conexão e de um
# Secret do Kubernetes; escaping é um bug esperando para acontecer, e 32 caracteres
# alfanuméricos já sobram em entropia.
resource "random_password" "database" {
  length  = 32
  special = false
}

# As mesmas subnets do node group: us-east-1e não oferta nem t3.medium nem db.t3.micro,
# e manter banco e nós nas mesmas AZs evita salto entre zonas em toda query.
resource "aws_db_subnet_group" "database" {
  name       = "${var.project_name}-db"
  subnet_ids = local.node_subnet_ids
}

resource "aws_security_group" "database" {
  name        = "${var.project_name}-db"
  description = "Postgres da oficina, alcancavel apenas pelos nos do cluster"
  vpc_id      = data.aws_vpc.default.id
}

# Referenciar o security group do cluster, e não um bloco CIDR, é o que mantém a regra
# correta mesmo se a VPC ou as subnets mudarem: quem entra é quem pertence ao cluster.
# O EKS anexa esse grupo aos nós do node group gerenciado.
resource "aws_vpc_security_group_ingress_rule" "database_from_cluster" {
  security_group_id            = aws_security_group.database.id
  description                  = "Postgres a partir dos nos do EKS"
  referenced_security_group_id = aws_eks_cluster.main.vpc_config[0].cluster_security_group_id
  ip_protocol                  = "tcp"
  from_port                    = local.postgres_port
  to_port                      = local.postgres_port
}

resource "aws_db_instance" "main" {
  identifier     = "${var.project_name}-db"
  engine         = "postgres"
  engine_version = var.database_engine_version
  instance_class = var.database_instance_class

  db_name  = var.database_name
  username = var.database_username
  password = random_password.database.result
  port     = local.postgres_port

  allocated_storage = var.database_allocated_storage
  storage_type      = "gp3"
  storage_encrypted = true

  db_subnet_group_name   = aws_db_subnet_group.database.name
  vpc_security_group_ids = [aws_security_group.database.id]
  publicly_accessible    = false
  multi_az               = false

  # Ambiente de avaliação, destruído ao fim de cada sessão do lab: backup e snapshot
  # final só custariam tempo de apply e de destroy.
  backup_retention_period = 0
  skip_final_snapshot     = true
  deletion_protection     = false
  apply_immediately       = true
}
