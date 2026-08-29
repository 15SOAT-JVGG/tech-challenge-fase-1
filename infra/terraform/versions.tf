terraform {
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }

  # O bucket de state nasce fora do Terraform, por infra/scripts/bootstrap-tf-state.sh:
  # o backend precisa existir antes do primeiro init. Em outra conta, rode o script e
  # troque o nome do bucket aqui — ele carrega o ID da conta porque o namespace do S3
  # é global.
  #
  # use_lockfile trava a execução pelo próprio S3, o que dispensa a tabela do DynamoDB
  # que a sessão do Learner Lab talvez não tenha permissão de criar.
  backend "s3" {
    bucket       = "oficina-mecanica-tf-state-059056506203"
    key          = "fase2/terraform.tfstate"
    region       = "us-east-1"
    encrypt      = true
    use_lockfile = true
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project   = var.project_name
      ManagedBy = "Terraform"
    }
  }
}
