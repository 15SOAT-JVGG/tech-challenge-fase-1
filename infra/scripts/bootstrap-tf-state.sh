#!/usr/bin/env bash
# Cria o bucket S3 que guarda o state do Terraform. É o único recurso desta fase que
# não nasce do Terraform: o backend precisa existir antes do primeiro `init`.
#
# Idempotente — rode quantas vezes quiser. O bucket sobrevive ao `terraform destroy`,
# então na prática isto é executado uma vez por conta.
#
# Requer uma sessão ativa do lab: exporte AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
# e AWS_SESSION_TOKEN, ou configure ~/.aws/credentials.
#
# Uso: infra/scripts/bootstrap-tf-state.sh
set -euo pipefail

REGION="${AWS_REGION:-${AWS_DEFAULT_REGION:-us-east-1}}"
BACKEND_FILE="$(cd "$(dirname "${BASH_SOURCE[0]}")/../terraform" && pwd)/versions.tf"

command -v aws >/dev/null || { echo "aws não encontrado" >&2; exit 1; }

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
# O namespace do S3 é global, então o nome carrega o ID da conta para não colidir.
BUCKET="${STATE_BUCKET:-oficina-mecanica-tf-state-${ACCOUNT_ID}}"

# Guarda antes de qualquer escrita: criar um bucket que o backend não usa só produz
# lixo na conta e a ilusão de que o init vai funcionar.
if ! grep -q "bucket *= *\"$BUCKET\"" "$BACKEND_FILE"; then
    echo "O backend em $BACKEND_FILE não aponta para $BUCKET." >&2
    echo "Ajuste o bloco backend \"s3\" antes de rodar este script." >&2
    exit 1
fi

if aws s3api head-bucket --bucket "$BUCKET" >/dev/null 2>&1; then
    echo "Bucket $BUCKET já existe."
else
    # us-east-1 é a única região que rejeita LocationConstraint.
    if [[ "$REGION" == "us-east-1" ]]; then
        aws s3api create-bucket --bucket "$BUCKET" --region "$REGION" >/dev/null
    else
        aws s3api create-bucket --bucket "$BUCKET" --region "$REGION" \
            --create-bucket-configuration "LocationConstraint=$REGION" >/dev/null
    fi
    echo "Bucket $BUCKET criado."
fi

# Versionamento é o que permite voltar a um state anterior quando um apply é
# interrompido no meio.
aws s3api put-bucket-versioning --bucket "$BUCKET" \
    --versioning-configuration Status=Enabled

aws s3api put-bucket-encryption --bucket "$BUCKET" \
    --server-side-encryption-configuration \
    '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'

# O state carrega a senha do RDS em texto claro; exposição pública seria vazamento.
aws s3api put-public-access-block --bucket "$BUCKET" \
    --public-access-block-configuration \
    'BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true'

echo "Versionamento, criptografia e bloqueio de acesso público aplicados."
echo "Pronto. Rode: terraform -chdir=infra/terraform init"
