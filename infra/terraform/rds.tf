locals {
  postgres_port = 5432

  database_name_prefix = "${var.project_name}-db"
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

# As mesmas subnets do node group, por dois motivos independentes. O primeiro é que
# us-east-1e também não serve para o banco — a AZ não oferta db.t3.micro, e um subnet
# group que a inclua deixa a criação da instância à mercê da AZ que o RDS sortear:
#
#   aws rds describe-orderable-db-instance-options --engine postgres \
#     --db-instance-class db.t3.micro \
#     --query 'OrderableDBInstanceOptions[0].AvailabilityZones[].Name'
#
# O segundo é que banco e nós nas mesmas AZs evitam salto entre zonas em toda query.
# Derivar a lista de um data source de RDS custaria mais de um minuto em cada plan.
resource "aws_db_subnet_group" "database" {
  name       = local.database_name_prefix
  subnet_ids = local.node_subnet_ids
}

# A descrição vai sem acento porque a AWS só aceita ASCII no campo, e é imutável:
# reescrevê-la substitui o security group e, junto, a regra de entrada.
resource "aws_security_group" "database" {
  name        = local.database_name_prefix
  description = "Postgres da oficina, alcancavel apenas pelos nos do cluster"
  vpc_id      = data.aws_vpc.default.id
}

# Referenciar o security group do cluster, e não um bloco CIDR, é o que mantém a regra
# correta mesmo se a VPC ou as subnets mudarem: quem entra é quem pertence ao cluster.
# O grupo cobre os nós e as ENIs do control plane, o que é mais do que os pods precisam,
# mas ainda é a fronteira mais estreita que o EKS oferece sem instalar nada.
#
# Sem regra de saída: o Postgres não inicia conexão.
resource "aws_vpc_security_group_ingress_rule" "database_from_cluster" {
  security_group_id            = aws_security_group.database.id
  description                  = "Postgres a partir dos nos do EKS"
  referenced_security_group_id = local.cluster_security_group_id
  ip_protocol                  = "tcp"
  from_port                    = local.postgres_port
  to_port                      = local.postgres_port
}

resource "aws_db_instance" "database" {
  identifier     = local.database_name_prefix
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

  # Ambiente de avaliação, destruído ao fim de cada sessão do lab: backup e snapshot
  # final só custariam tempo de apply e de destroy, e esperar a janela de manutenção
  # para uma alteração custaria uma sessão inteira.
  backup_retention_period = 0
  skip_final_snapshot     = true
  apply_immediately       = true
}
