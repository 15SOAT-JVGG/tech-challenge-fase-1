terraform {
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }

  # Mesmo bucket da stack da plataforma (infra/terraform), outra chave: a função tem ciclo de
  # entrega próprio e não deve disputar o lock do state do cluster a cada deploy de código.
  backend "s3" {
    bucket       = "oficina-mecanica-tf-state-059056506203"
    key          = "customer-auth/terraform.tfstate"
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
      Component = "customer-auth"
      ManagedBy = "Terraform"
    }
  }
}
