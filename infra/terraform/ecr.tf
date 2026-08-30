resource "aws_ecr_repository" "app" {
  name = var.project_name

  # A tag que o cluster consome é o SHA do commit, que nunca é reescrito. Imutável
  # ainda assim quebraria a publicação, porque cada push move também a tag `latest`.
  image_tag_mutability = "MUTABLE"

  # O ambiente é destruído ao fim de cada sessão do lab, e o destroy de um repositório
  # com imagem dentro falha sem isto.
  force_delete = true

  image_scanning_configuration {
    scan_on_push = true
  }
}

# Cada merge publica uma imagem nova. Sem expiração, o repositório cresce sem limite.
resource "aws_ecr_lifecycle_policy" "app" {
  repository = aws_ecr_repository.app.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Mantém as ${var.image_retention_count} imagens mais recentes"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = var.image_retention_count
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}
