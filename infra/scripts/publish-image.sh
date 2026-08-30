#!/usr/bin/env bash
# Constrói a imagem da aplicação e publica no ECR criado pelo Terraform.
#
# A tag é o SHA do commit, para que o rollout no cluster seja sempre uma mudança real
# de imagem; `latest` acompanha por conveniência de inspeção manual.
#
# A plataforma é fixada em linux/amd64: os nós do EKS são t3.medium (x86_64) e uma
# imagem construída num Mac ARM sem essa flag sobe, mas nenhum pod inicia.
#
# Requer uma sessão ativa do lab e o repositório já provisionado
# (`terraform -chdir=infra/terraform apply`).
#
# Uso: infra/scripts/publish-image.sh [tag]
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TERRAFORM_DIR="$REPO_ROOT/infra/terraform"
REGION="${AWS_REGION:-${AWS_DEFAULT_REGION:-us-east-1}}"

for tool in aws docker git; do
    command -v "$tool" >/dev/null || { echo "$tool não encontrado" >&2; exit 1; }
done

TAG="${1:-$(git -C "$REPO_ROOT" rev-parse HEAD)}"

# Em CI o repositório vem por variável de ambiente, sem custo de um init do Terraform.
if [[ -z "${ECR_REPOSITORY_URL:-}" ]]; then
    command -v terraform >/dev/null || { echo "terraform não encontrado" >&2; exit 1; }
    ECR_REPOSITORY_URL="$(terraform -chdir="$TERRAFORM_DIR" output -raw ecr_repository_url)"
fi
REGISTRY="${ECR_REPOSITORY_URL%%/*}"

echo "Publicando $ECR_REPOSITORY_URL:$TAG"

aws ecr get-login-password --region "$REGION" \
    | docker login --username AWS --password-stdin "$REGISTRY"

# Sem --provenance=false o buildx publica um índice OCI com manifestos filhos sem tag,
# que a política de ciclo de vida do ECR contaria como imagens e poderia expirar por
# baixo de uma tag ainda em uso.
docker buildx build \
    --platform linux/amd64 \
    --provenance=false \
    --file "$REPO_ROOT/infra/docker/Dockerfile" \
    --tag "$ECR_REPOSITORY_URL:$TAG" \
    --tag "$ECR_REPOSITORY_URL:latest" \
    --push \
    "$REPO_ROOT"

echo "Publicado: $ECR_REPOSITORY_URL:$TAG"
