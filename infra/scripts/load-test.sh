#!/usr/bin/env bash
# Provoca carga sintética contra o ambiente implantado, para demonstrar a escala
# automática: enquanto ele roda, o HPA vê a CPU dos pods subir e cria réplicas.
#
# A carga é uma rajada de logins. O endpoint é o mais caro em processador que a API
# tem — a verificação BCrypt é lenta de propósito —, então algumas dezenas de
# requisições concorrentes já levam um pod acima do alvo do autoscaler, sem precisar
# de gerador de carga instalado na máquina. Só curl.
#
# O script não afirma nada sobre o cluster: quem observa a escala é quem assiste,
# noutro terminal, a `kubectl get hpa,pods -w`. O procedimento inteiro está em
# infra/README.md, na seção "Escalabilidade automática".
#
# Uso:
#   infra/scripts/load-test.sh                       # descobre o endereço do Service no cluster
#   infra/scripts/load-test.sh http://localhost:8080 # ou um endereço explícito
#
# Requer curl e jq, mais kubectl quando o endereço não é informado. E requer
# APP_SEED_ADMIN_PASSWORD — a mesma senha entregue ao cluster por
# infra/scripts/create-app-credentials.sh. APP_SEED_ADMIN_USERNAME é opcional
# (default: admin), LOAD_DURATION_SECONDS e LOAD_CONCURRENCY ajustam a rajada.
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib/environment.sh"

DURATION_SECONDS="${LOAD_DURATION_SECONDS:-300}"
CONCURRENCY="${LOAD_CONCURRENCY:-40}"
REQUEST_TIMEOUT_SECONDS=30
PROGRESS_INTERVAL_SECONDS=15

require_tools curl jq
require_seed_admin_password
resolve_base_url "${1:-}"

# Zero conexões não geraria carga, e zero segundos dividiria por zero no resumo.
require_positive() {
    local name="$1" value="$2"
    ((value > 0)) && return

    echo "$name precisa ser ao menos 1, e veio $value." >&2
    exit 1
}

require_positive LOAD_DURATION_SECONDS "$DURATION_SECONDS"
require_positive LOAD_CONCURRENCY "$CONCURRENCY"

LOGIN_PAYLOAD="$(login_payload)"

# Devolve o código HTTP em vez de um token: aqui interessa o custo da requisição, não
# a sessão que ela abre.
attempt_login() {
    curl --silent --show-error --output /dev/null --write-out '%{http_code}' \
        --max-time "$REQUEST_TIMEOUT_SECONDS" \
        --request POST "$BASE_URL/v1/auth/login" \
        --header 'Content-Type: application/json' \
        --header 'Accept: application/json' \
        --data "$LOGIN_PAYLOAD"
}

# Uma requisição antes da rajada. Credencial errada ou ambiente fora do ar viram uma
# mensagem única aqui, em vez de milhares de 401 que ainda gastariam CPU e dariam uma
# demonstração enganosa.
PREFLIGHT_STATUS="$(attempt_login || true)"
if [[ "$PREFLIGHT_STATUS" != "200" ]]; then
    echo "FALHA: o login de verificação em $BASE_URL devolveu $PREFLIGHT_STATUS, esperado 200." >&2
    echo "Confira o endereço e APP_SEED_ADMIN_PASSWORD antes de gerar carga." >&2
    exit 1
fi

REQUEST_COUNT_DIR="$(mktemp -d)"
WORKER_PIDS=()

# O `|| true` não é decoração: na saída normal os workers já terminaram, e um kill que
# falha aborta a própria limpeza sob `set -e`, deixando o diretório temporário para
# trás e o script saindo com erro depois de uma execução boa.
cleanup() {
    if [[ ${#WORKER_PIDS[@]} -gt 0 ]]; then
        kill "${WORKER_PIDS[@]}" 2>/dev/null || true
    fi
    rm -rf "$REQUEST_COUNT_DIR"
}
trap cleanup EXIT

# Cada worker é um laço serial de requisições; a concorrência vem de haver muitos
# deles. O prazo é um instante absoluto porque SECONDS não conta o mesmo dentro de um
# subshell, e o total de cada worker vai para um arquivo nomeado pelo índice porque
# subshell não devolve variável ao processo pai.
worker() {
    local index="$1" deadline="$2" requests=0
    while (($(date +%s) < deadline)); do
        attempt_login >/dev/null 2>&1 || true
        requests=$((requests + 1))
    done
    printf '%s' "$requests" >"$REQUEST_COUNT_DIR/$index"
}

echo "Carga contra $BASE_URL: $CONCURRENCY conexões por ${DURATION_SECONDS}s"
echo "Acompanhe a escala noutro terminal: kubectl get hpa,pods -w"
echo

DEADLINE=$(($(date +%s) + DURATION_SECONDS))
for ((connection = 0; connection < CONCURRENCY; connection++)); do
    worker "$connection" "$DEADLINE" &
    WORKER_PIDS+=($!)
done

REMAINING_SECONDS=$((DEADLINE - $(date +%s)))
while ((REMAINING_SECONDS > 0)); do
    sleep $((REMAINING_SECONDS < PROGRESS_INTERVAL_SECONDS ? REMAINING_SECONDS : PROGRESS_INTERVAL_SECONDS))
    REMAINING_SECONDS=$((DEADLINE - $(date +%s)))
    ((REMAINING_SECONDS > 0)) && echo "  ${REMAINING_SECONDS}s restantes"
done
wait "${WORKER_PIDS[@]}"

TOTAL_REQUESTS=0
for count_file in "$REQUEST_COUNT_DIR"/*; do
    TOTAL_REQUESTS=$((TOTAL_REQUESTS + $(cat "$count_file")))
done

echo
echo "Carga encerrada: $TOTAL_REQUESTS logins em ${DURATION_SECONDS}s (~$((TOTAL_REQUESTS / DURATION_SECONDS))/s)"
echo "As réplicas criadas voltam a diminuir sozinhas nos minutos seguintes."
