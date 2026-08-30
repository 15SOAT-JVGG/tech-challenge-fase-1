# shellcheck shell=bash
#
# Funções que todo script que fala com o ambiente implantado precisa antes de dizer
# qualquer coisa útil: conferir as ferramentas, exigir a credencial de seed e descobrir
# o endereço público da API.
#
# Ficam aqui porque smoke-test.sh e load-test.sh fazem os três passos do mesmo jeito, e
# a parte que mais importa deles são as mensagens de erro — que precisam dizer ao
# operador o que fazer em seguida, e não podem divergir entre um script e outro.
#
# Para ser incluída, não executada:
#   source "$(dirname "${BASH_SOURCE[0]}")/lib/environment.sh"
#
# Quem inclui recebe também SERVICE_NAME, ADMIN_USERNAME e BASE_URL.

SERVICE_NAME="oficina-mecanica"
ADMIN_USERNAME="${APP_SEED_ADMIN_USERNAME:-admin}"

require_tools() {
    local tool
    for tool in "$@"; do
        command -v "$tool" >/dev/null || { echo "$tool não encontrado" >&2; exit 1; }
    done
}

require_seed_admin_password() {
    [[ -n "${APP_SEED_ADMIN_PASSWORD:-}" ]] && return

    echo "APP_SEED_ADMIN_PASSWORD não definida." >&2
    echo "É a senha do usuário de seed entregue ao cluster no Secret oficina-mecanica-env:" >&2
    echo "  export APP_SEED_ADMIN_PASSWORD=..." >&2
    exit 1
}

# Com um endereço explícito, é ele. Sem, o endereço sai do balanceador do Service — o
# que dispensa o operador de copiar à mão um hostname de ELB a cada ambiente novo.
# BASE_URL é o resultado, lido por quem incluiu este arquivo.
# shellcheck disable=SC2034
resolve_base_url() {
    local given_url="${1:-}" elb_hostname

    if [[ -n "$given_url" ]]; then
        BASE_URL="${given_url%/}"
        return
    fi

    require_tools kubectl
    elb_hostname="$(kubectl get svc "$SERVICE_NAME" -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')"
    if [[ -z "$elb_hostname" ]]; then
        echo "O Service $SERVICE_NAME ainda não tem endereço de balanceador." >&2
        echo "Aguarde o provisionamento do ELB (cerca de um minuto) ou informe o endereço:" >&2
        echo "  $0 http://meu-endereco" >&2
        exit 1
    fi
    BASE_URL="http://$elb_hostname"
}

login_payload() {
    jq -nc \
        --arg username "$ADMIN_USERNAME" \
        --arg password "$APP_SEED_ADMIN_PASSWORD" \
        '{username: $username, password: $password}'
}
