#!/usr/bin/env bash
# Cria no cluster os dois Secrets que o Deployment consome. Eles não estão em k8s/ de
# propósito: senha do banco, credenciais de seed e chave privada RS256 não entram no
# repositório.
#
#   oficina-mecanica-env  variáveis sensíveis, injetadas por envFrom
#   oficina-mecanica-jwt  o par RS256, montado como arquivo em /etc/jwt
#
# Cada valor tem duas origens, e é o que permite o mesmo script servir ao operador e à
# pipeline — que não tem credencial da AWS para ler o state do Terraform (ADR-0002):
#
#   usuário e senha do banco  POSTGRES_USERNAME / POSTGRES_PASSWORD quando definidos,
#                             senão os outputs do Terraform
#   par RS256                 JWT_PRIVATE_KEY_B64 / JWT_PUBLIC_KEY_B64 quando definidos,
#                             senão um diretório local gerado por
#                             infra/scripts/generate-jwt-pair.sh
#
# Idempotente: reescreve os dois a cada execução. Reiniciar os pods depois é de quem
# chama (`kubectl rollout restart deploy/oficina-mecanica`).
#
# Uso: infra/scripts/create-app-credentials.sh [diretório-do-par-rs256]
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TERRAFORM_DIR="$REPO_ROOT/infra/terraform"
CONFIGMAP_FILE="$REPO_ROOT/k8s/configmap.yaml"
JWT_DIR="${1:-$REPO_ROOT/.local-jwt}"

ENV_SECRET="oficina-mecanica-env"
JWT_SECRET="oficina-mecanica-jwt"
PRIVATE_PEM="privateKey.pem"
PUBLIC_PEM="publicKey.pem"

command -v kubectl >/dev/null || { echo "kubectl não encontrado" >&2; exit 1; }

for required in APP_SEED_ADMIN_PASSWORD APP_SEED_MECHANIC_PASSWORD; do
    if [[ -z "${!required:-}" ]]; then
        echo "$required não definida." >&2
        echo "Sem ela a aplicação sobe sem usuário para autenticar. Exporte as duas:" >&2
        echo "  export APP_SEED_ADMIN_PASSWORD=... APP_SEED_MECHANIC_PASSWORD=..." >&2
        exit 1
    fi
done

# Uma das duas definida é sempre engano — secret do repositório que ficou de fora, ou
# variável com nome errado. Sem este guarda, o caso cairia no Terraform e morreria com
# "terraform não encontrado", que não diz nada sobre a causa.
if [[ -n "${POSTGRES_USERNAME:-}" || -n "${POSTGRES_PASSWORD:-}" ]]; then
    for required in POSTGRES_USERNAME POSTGRES_PASSWORD; do
        if [[ -z "${!required:-}" ]]; then
            echo "$required não definida, e a outra credencial do banco está." >&2
            echo "As duas andam juntas: defina as duas, ou nenhuma para ler do Terraform." >&2
            exit 1
        fi
    done
    DATABASE_USERNAME="$POSTGRES_USERNAME"
    DATABASE_PASSWORD="$POSTGRES_PASSWORD"
    echo "Credenciais do banco lidas das variáveis de ambiente."
else
    command -v terraform >/dev/null || { echo "terraform não encontrado" >&2; exit 1; }
    DATABASE_HOST="$(terraform -chdir="$TERRAFORM_DIR" output -raw database_host)"
    DATABASE_USERNAME="$(terraform -chdir="$TERRAFORM_DIR" output -raw database_username)"
    DATABASE_PASSWORD="$(terraform -chdir="$TERRAFORM_DIR" output -raw database_password)"

    # O endereço do banco é configuração não sensível e por isso mora no ConfigMap, que é
    # versionado. Recriar a instância troca esse endereço, e o sintoma seria um pod que
    # nunca fica pronto — comparar aqui transforma isso numa mensagem de erro. A
    # comparação só é possível com o Terraform à mão; no caminho de entrega, quem acusa
    # ConfigMap desatualizado é o `rollout status`, que não converge porque o pod não
    # alcança o banco.
    CONFIGURED_HOST="$(sed -n 's/^ *INFRA_HOST_POSTGRES: *//p' "$CONFIGMAP_FILE")"
    if [[ "$CONFIGURED_HOST" != "$DATABASE_HOST" ]]; then
        echo "O ConfigMap não aponta para o banco atual." >&2
        echo "  Terraform: $DATABASE_HOST" >&2
        echo "  ConfigMap: $CONFIGURED_HOST" >&2
        echo "Atualize INFRA_HOST_POSTGRES em k8s/configmap.yaml e rode de novo." >&2
        exit 1
    fi
fi

# Em CI o par chega por variável de ambiente; localmente, por arquivo.
JWT_WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$JWT_WORK_DIR"' EXIT

if [[ -n "${JWT_PRIVATE_KEY_B64:-}" && -n "${JWT_PUBLIC_KEY_B64:-}" ]]; then
    base64 --decode <<<"$JWT_PRIVATE_KEY_B64" >"$JWT_WORK_DIR/$PRIVATE_PEM"
    base64 --decode <<<"$JWT_PUBLIC_KEY_B64" >"$JWT_WORK_DIR/$PUBLIC_PEM"
    echo "Par RS256 lido das variáveis de ambiente."
elif [[ -s "$JWT_DIR/$PRIVATE_PEM" && -s "$JWT_DIR/$PUBLIC_PEM" ]]; then
    cp "$JWT_DIR/$PRIVATE_PEM" "$JWT_DIR/$PUBLIC_PEM" "$JWT_WORK_DIR/"
    echo "Par RS256 lido de $JWT_DIR."
else
    echo "Par RS256 não encontrado." >&2
    echo "Defina JWT_PRIVATE_KEY_B64 e JWT_PUBLIC_KEY_B64, ou gere um par local:" >&2
    echo "  infra/scripts/generate-jwt-pair.sh" >&2
    exit 1
fi

# `kubectl create` sozinho falha quando o objeto já existe; passar por --dry-run=client
# e aplicar é o que torna a reexecução possível.
kubectl create secret generic "$ENV_SECRET" \
    --from-literal=POSTGRES_USERNAME="$DATABASE_USERNAME" \
    --from-literal=POSTGRES_PASSWORD="$DATABASE_PASSWORD" \
    --from-literal=APP_SEED_ADMIN_PASSWORD="$APP_SEED_ADMIN_PASSWORD" \
    --from-literal=APP_SEED_MECHANIC_PASSWORD="$APP_SEED_MECHANIC_PASSWORD" \
    --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic "$JWT_SECRET" \
    --from-file="$PRIVATE_PEM=$JWT_WORK_DIR/$PRIVATE_PEM" \
    --from-file="$PUBLIC_PEM=$JWT_WORK_DIR/$PUBLIC_PEM" \
    --dry-run=client -o yaml | kubectl apply -f -

echo "$ENV_SECRET e $JWT_SECRET aplicados."
