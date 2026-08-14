#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_DIR
readonly BOOTSTRAP_DIR="${SCRIPT_DIR}/bootstrap"
readonly OPERATION="${1:-plan}"
readonly AWS_REGION="${AWS_REGION:-us-east-1}"
readonly TF_STATE_KEY="${TF_STATE_KEY:-tech-challenge-fase-1/dev/terraform.tfstate}"
PLAN_FILE=""

export AWS_REGION

cleanup() {
  if [[ -n "${PLAN_FILE}" ]]; then
    rm -f -- "${PLAN_FILE}"
  fi
  unset TF_VAR_aws_region
}

trap cleanup EXIT

usage() {
  cat <<'USAGE'
Uso:
  ./infra/terraform/run-local.sh bootstrap  # cria bucket S3 e role OIDC (uma vez)
  ./infra/terraform/run-local.sh plan       # mostra as mudanças da infraestrutura
  ./infra/terraform/run-local.sh apply      # planeja e aplica após confirmação

Variáveis opcionais:
  AWS_PROFILE       Perfil local da AWS, preferencialmente configurado com SSO
  AWS_REGION        Região AWS (padrão: us-east-1)
  TF_STATE_BUCKET   Bucket do state; se omitido, tenta ler o output do bootstrap
  TF_STATE_KEY      Chave do state no S3

USAGE
}

fail() {
  printf 'Erro: %s\n' "$*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "comando '$1' não encontrado."
}

confirm_apply() {
  local confirmation

  printf '\nEsta operação cria ou altera recursos cobrados pela AWS.\n'
  read -r -p "Digite 'aplicar' para continuar: " confirmation
  [[ "${confirmation}" == "aplicar" ]] || fail "operação cancelada."
}

check_aws_access() {
  printf 'Validando identidade AWS'
  if [[ -n "${AWS_PROFILE:-}" ]]; then
    printf ' com o perfil %s' "${AWS_PROFILE}"
  fi
  printf '...\n'

  if ! aws sts get-caller-identity >/dev/null; then
    if [[ -n "${AWS_PROFILE:-}" ]]; then
      fail "não foi possível autenticar. Execute: aws sso login --profile ${AWS_PROFILE}"
    fi
    fail "não foi possível autenticar. Configure AWS SSO ou exporte AWS_PROFILE."
  fi

  aws sts get-caller-identity --output table
}

resolve_state_bucket() {
  if [[ -n "${TF_STATE_BUCKET:-}" ]]; then
    printf '%s' "${TF_STATE_BUCKET}"
    return
  fi

  terraform -chdir="${BOOTSTRAP_DIR}" output -raw state_bucket_name 2>/dev/null || true
}

run_bootstrap() {
  local state_bucket="${TF_STATE_BUCKET:-}"

  if [[ -z "${state_bucket}" ]]; then
    read -r -p "Nome globalmente único do bucket S3 para o state: " state_bucket
  fi
  [[ -n "${state_bucket}" ]] || fail "o nome do bucket S3 é obrigatório."

  PLAN_FILE="$(mktemp "${TMPDIR:-/tmp}/oficina-bootstrap.XXXXXX.tfplan")"

  terraform -chdir="${BOOTSTRAP_DIR}" fmt -check
  terraform -chdir="${BOOTSTRAP_DIR}" init -input=false
  terraform -chdir="${BOOTSTRAP_DIR}" validate
  terraform -chdir="${BOOTSTRAP_DIR}" plan \
    -input=false \
    -var="aws_region=${AWS_REGION}" \
    -var="state_bucket_name=${state_bucket}" \
    -out="${PLAN_FILE}"

  confirm_apply
  terraform -chdir="${BOOTSTRAP_DIR}" apply -input=false "${PLAN_FILE}"

  printf '\nOutputs para configurar o GitHub Environment aws:\n'
  terraform -chdir="${BOOTSTRAP_DIR}" output
}

run_main_terraform() {
  local state_bucket

  state_bucket="$(resolve_state_bucket)"
  if [[ -z "${state_bucket}" ]]; then
    fail "bucket do state não encontrado. Execute o bootstrap ou exporte TF_STATE_BUCKET."
  fi

  export TF_VAR_aws_region="${AWS_REGION}"

  terraform -chdir="${SCRIPT_DIR}" fmt -check -recursive
  terraform -chdir="${SCRIPT_DIR}" init -input=false \
    -backend-config="bucket=${state_bucket}" \
    -backend-config="key=${TF_STATE_KEY}" \
    -backend-config="region=${AWS_REGION}"
  terraform -chdir="${SCRIPT_DIR}" validate

  if [[ "${OPERATION}" == "plan" ]]; then
    terraform -chdir="${SCRIPT_DIR}" plan -input=false -lock-timeout=5m
    return
  fi

  PLAN_FILE="$(mktemp "${TMPDIR:-/tmp}/oficina-main.XXXXXX.tfplan")"

  terraform -chdir="${SCRIPT_DIR}" plan \
    -input=false \
    -lock-timeout=5m \
    -out="${PLAN_FILE}"

  confirm_apply
  terraform -chdir="${SCRIPT_DIR}" apply -input=false "${PLAN_FILE}"

  printf '\nInfraestrutura aplicada com sucesso:\n'
  terraform -chdir="${SCRIPT_DIR}" output
}

case "${OPERATION}" in
  bootstrap | plan | apply) ;;
  -h | --help | help)
    usage
    exit 0
    ;;
  *)
    usage >&2
    fail "operação '${OPERATION}' inválida."
    ;;
esac

require_command aws
require_command terraform
check_aws_access

if [[ "${OPERATION}" == "bootstrap" ]]; then
  run_bootstrap
else
  run_main_terraform
fi
