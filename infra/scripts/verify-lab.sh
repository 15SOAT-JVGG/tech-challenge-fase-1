#!/usr/bin/env bash
# Verifica o que a conta do AWS Academy Learner Lab permite antes de escrever ou
# aplicar Terraform. Rode de novo sempre que o lab for reprovisionado: os nomes das
# roles e os IDs de VPC e subnet mudam junto, e o HCL depende deles.
#
# Requer uma sessão ativa do lab: exporte AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
# e AWS_SESSION_TOKEN, ou configure ~/.aws/credentials.
#
# Uso: infra/scripts/verify-lab.sh [caminho-do-relatorio]
# Saída: relatório markdown (default: ./lab-capabilities.md). O relatório contém ID
# de conta, ARNs e IDs de subnet, então ele é ignorado pelo git de propósito.
#
# Nenhum probe cria recurso pago. Para EKS e RDS a chamada de escrita recebe um
# parâmetro inexistente de propósito, então ela passa pela autorização IAM e morre
# na validação. O único probe que cria algo de verdade é o do ECR, onde um
# repositório vazio é gratuito e instantâneo — e ele é removido em seguida.
set -uo pipefail

REGION="${AWS_REGION:-${AWS_DEFAULT_REGION:-us-east-1}}"
NODE_INSTANCE_TYPE="${NODE_INSTANCE_TYPE:-t3.medium}"
RDS_CLASSES="${RDS_CLASSES:-db.t3.micro db.t4g.micro db.t3.small db.t3.medium db.t3.large db.m5.large}"
REPORT="${1:-lab-capabilities.md}"
PROBE_NAME="soat-probe-$$"
ABSENT_SUBNET="subnet-0123456789abcdef0"
ABSENT_SUBNET_GROUP="soat-definitely-nonexistent-probe"

for tool in aws jq openssl; do
    command -v "$tool" >/dev/null || { echo "$tool não encontrado" >&2; exit 1; }
done

# A autorização é avaliada na borda da API, antes do processamento do request, então
# AccessDenied aparece antes de qualquer erro de validação.
#
# O veredito exige evidência positiva: só um erro vindo do serviço prova que a chamada
# atravessou a autorização. Falha de rede, throttling ou opção de CLI errada não provam
# nada e viram INDETERMINADO — o default não pode ser PERMITIDO, senão um probe quebrado
# passa por permissão concedida.
classify_authorization() {
    local output="$1"
    case "$output" in
        *AccessDenied*|*UnauthorizedOperation*|*"not authorized"*|*"explicit deny"*|*"implicit deny"*)
            echo "BLOQUEADO"; return ;;
    esac
    case "$output" in
        *ValidationException*|*ValidationError*|*InvalidParameter*|*NotFound*|*"does not exist"*|*"not found"*|*AlreadyExists*)
            echo "PERMITIDO" ;;
        *)
            echo "INDETERMINADO" ;;
    esac
}

# O probe do ECR cria um repositório de verdade. Se a sessão morrer entre o create e o
# delete, o trap ainda o remove.
PROBE_ECR_CREATED=""
cleanup_probes() {
    if [[ -n "$PROBE_ECR_CREATED" ]]; then
        aws ecr delete-repository --region "$REGION" \
            --repository-name "$PROBE_NAME" --force >/dev/null 2>&1
    fi
}
trap cleanup_probes EXIT INT TERM

section() { printf '\n## %s\n\n' "$1" >>"$REPORT"; }
line() { printf '%s\n' "$1" >>"$REPORT"; }
fence() { printf '\n```\n%s\n```\n' "$1" >>"$REPORT"; }

: >"$REPORT" || { echo "não foi possível escrever em $REPORT" >&2; exit 1; }
line "# Capacidades da conta do AWS Academy Learner Lab"
line ""
line "Coletado em $(date -u '+%Y-%m-%d %H:%M:%SZ') na região \`$REGION\`."
line "Gerado por \`infra/scripts/verify-lab.sh\`."

# --- Identidade e conta ------------------------------------------------------
section "Identidade e conta"
identity="$(aws sts get-caller-identity --output json 2>&1)"
if ! grep -q '"Account"' <<<"$identity"; then
    line "**Sessão inválida ou expirada.** Reinicie o lab e reexporte as credenciais."
    fence "$identity"
    echo "Sessão inválida. Relatório parcial em $REPORT" >&2
    exit 1
fi
ACCOUNT_ID="$(jq -r .Account <<<"$identity")"
line "- Conta: \`$ACCOUNT_ID\`"
line "- Identidade da sessão: \`$(jq -r .Arn <<<"$identity")\`"
line "- Região: \`$REGION\`"
line ""
line "A sessão do lab é quem roda o \`terraform apply\`, então são as permissões dela"
line "que decidem o que a IaC consegue provisionar."

# --- Roles -------------------------------------------------------------------
section "Roles utilizáveis"
# A LabRole confia em ~50 serviços; só os desta fase são relevantes no relatório.
RELEVANT_SERVICES='eks|ec2|rds|elasticloadbalancing'
describe_role() {
    local role="$1" policies services
    # As VocLabPolicy* entram com nome gerado pela stack do lab e têm leitura negada,
    # então são omitidas: só as políticas gerenciadas dizem algo verificável.
    policies="$(aws iam list-attached-role-policies --role-name "$role" \
        --query 'AttachedPolicies[].PolicyName' --output text 2>/dev/null \
        | tr '\t' '\n' | grep -v '^c[0-9]' | paste -sd' ' -)"
    services="$(aws iam get-role --role-name "$role" \
        --query 'Role.AssumeRolePolicyDocument.Statement[].Principal.Service' --output json 2>/dev/null \
        | jq -r "[.. | strings | select(test(\"^($RELEVANT_SERVICES)\\\\.\"))] | sort | join(\", \")" 2>/dev/null)"
    line "| \`$role\` | ${services:-—} | ${policies:-—} |"
}

line "Serviços de confiança filtrados para os desta fase; políticas \`VocLabPolicy*\` omitidas"
line "(têm \`iam:GetPolicy\` negado, então o conteúdo delas não é auditável)."
line ""
line "| Role | Confia em (relevantes) | Políticas gerenciadas |"
line "|---|---|---|"
LAB_ROLES="$(aws iam list-roles --query 'Roles[?contains(RoleName,`Lab`)].RoleName' --output text 2>/dev/null | tr '\t' '\n')"
for role in $LAB_ROLES; do
    [[ "$role" == RoleForLambdaModLabRole ]] && continue
    describe_role "$role"
done

# Os nomes das roles de EKS carregam um prefixo gerado pela stack do lab, que muda a
# cada reprovisionamento — o Terraform precisa resolvê-las por regex, não por ARN fixo.
EKS_CLUSTER_ROLE="$(grep 'LabEksClusterRole' <<<"$LAB_ROLES" | head -1)"
EKS_NODE_ROLE="$(grep 'LabEksNodeRole' <<<"$LAB_ROLES" | head -1)"
LABROLE_ARN="arn:aws:iam::${ACCOUNT_ID}:role/LabRole"
line ""
line "- \`LabRole\` ARN: \`$LABROLE_ARN\`"
if [[ -n "$EKS_CLUSTER_ROLE" && -n "$EKS_NODE_ROLE" ]]; then
    line "- Role de cluster dedicada: \`$EKS_CLUSTER_ROLE\`"
    line "- Role de node dedicada: \`$EKS_NODE_ROLE\`"
    line "- O prefixo dessas duas é gerado pela stack do lab e muda a cada reprovisionamento."
    line "  No Terraform, resolva por \`data \"aws_iam_roles\"\` com \`name_regex\`, nunca por ARN fixo."
else
    line "- Nenhuma role dedicada de EKS encontrada; a \`LabRole\` é a única opção."
fi

# --- VPCs e subnets ----------------------------------------------------------
section "VPCs e subnets"
line "| VPC | CIDR | Default | Name |"
line "|---|---|---|---|"
aws ec2 describe-vpcs --region "$REGION" \
    --query 'Vpcs[].[VpcId,CidrBlock,IsDefault,Tags[?Key==`Name`]|[0].Value]' \
    --output text 2>/dev/null \
    | awk -F'\t' '{printf "| `%s` | `%s` | %s | %s |\n", $1, $2, $3, $4}' >>"$REPORT"

VPC_ID="$(aws ec2 describe-vpcs --region "$REGION" \
    --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text 2>/dev/null)"

# O node group falha se receber uma subnet numa AZ que não oferece o tipo de instância.
AZS_WITH_TYPE="$(aws ec2 describe-instance-type-offerings --region "$REGION" \
    --location-type availability-zone \
    --filters "Name=instance-type,Values=$NODE_INSTANCE_TYPE" \
    --query 'InstanceTypeOfferings[].Location' --output text 2>/dev/null | tr '\t' ' ')"

line ""
line "### Subnets da VPC default (\`$VPC_ID\`)"
line ""
line "| Subnet | AZ | CIDR | IP público | Oferece \`$NODE_INSTANCE_TYPE\` |"
line "|---|---|---|---|---|"
USABLE_SUBNETS=""
while IFS=$'\t' read -r subnet_id availability_zone cidr_block maps_public_ip; do
    [[ -z "$subnet_id" ]] && continue
    if [[ " $AZS_WITH_TYPE " == *" $availability_zone "* ]]; then
        offers="sim"; USABLE_SUBNETS="${USABLE_SUBNETS:+$USABLE_SUBNETS,}$subnet_id"
    else
        offers="**não**"
    fi
    line "| \`$subnet_id\` | $availability_zone | \`$cidr_block\` | $maps_public_ip | $offers |"
done < <(aws ec2 describe-subnets --region "$REGION" \
    --filters "Name=vpc-id,Values=$VPC_ID" \
    --query 'sort_by(Subnets,&AvailabilityZone)[].[SubnetId,AvailabilityZone,CidrBlock,MapPublicIpOnLaunch]' \
    --output text 2>/dev/null)

line ""
line "Subnets aptas a receber node group (\`$NODE_INSTANCE_TYPE\`):"
line ""
line "\`\`\`"
line "$USABLE_SUBNETS"
line "\`\`\`"

# --- EKS ---------------------------------------------------------------------
section "EKS"
eks_list="$(aws eks list-clusters --region "$REGION" --output json 2>&1)"
if grep -q '"clusters"' <<<"$eks_list"; then
    line "- \`eks:ListClusters\`: PERMITIDO — clusters existentes: \`$(jq -c .clusters <<<"$eks_list")\`"
    for cluster_name in $(jq -r '.clusters[]' <<<"$eks_list"); do
        line "  - \`$cluster_name\`: $(aws eks describe-cluster --region "$REGION" --name "$cluster_name" \
            --query 'cluster.[status,version,resourcesVpcConfig.vpcId]' --output text 2>/dev/null | tr '\t' ' ')"
    done
else
    line "- \`eks:ListClusters\`: **$(classify_authorization "$eks_list")**"
    fence "$eks_list"
fi

eks_probe="$(aws eks create-cluster --region "$REGION" --name "$PROBE_NAME" \
    --role-arn "$LABROLE_ARN" --resources-vpc-config "subnetIds=$ABSENT_SUBNET" 2>&1)"
line "- \`eks:CreateCluster\`: **$(classify_authorization "$eks_probe")**"

ng_probe="$(aws eks create-nodegroup --region "$REGION" \
    --cluster-name "$PROBE_NAME" --nodegroup-name probe \
    --node-role "$LABROLE_ARN" --subnets "$ABSENT_SUBNET" 2>&1)"
line "- \`eks:CreateNodegroup\`: **$(classify_authorization "$ng_probe")**"

# --- RDS ---------------------------------------------------------------------
section "RDS"
rds_list="$(aws rds describe-db-instances --region "$REGION" \
    --query 'DBInstances[].DBInstanceIdentifier' --output json 2>&1)"
if grep -q '^\[' <<<"$rds_list"; then
    line "- \`rds:DescribeDBInstances\`: PERMITIDO — instâncias existentes: \`$(jq -c . <<<"$rds_list")\`"
else
    line "- \`rds:DescribeDBInstances\`: **$(classify_authorization "$rds_list")**"
fi

sg_probe="$(aws rds create-db-subnet-group --region "$REGION" \
    --db-subnet-group-name "$PROBE_NAME" --db-subnet-group-description probe \
    --subnet-ids "$ABSENT_SUBNET" 2>&1)"
line "- \`rds:CreateDBSubnetGroup\`: **$(classify_authorization "$sg_probe")**"

# O probe por classe só é seguro enquanto esse subnet group não existir: ele é o que faz
# a chamada morrer na validação. Se existisse, a primeira classe autorizada criaria uma
# instância paga de verdade, que o script não remove.
if aws rds describe-db-subnet-groups --region "$REGION" \
    --db-subnet-group-name "$ABSENT_SUBNET_GROUP" >/dev/null 2>&1; then
    echo "ABORTANDO: o subnet group '$ABSENT_SUBNET_GROUP' existe nesta conta." >&2
    echo "O probe de classe do RDS criaria uma instância paga. Ajuste ABSENT_SUBNET_GROUP." >&2
    line ""
    line "**Probe de classe do RDS não executado:** o subnet group \`$ABSENT_SUBNET_GROUP\`"
    line "existe nesta conta, então o probe deixaria de ser seguro."
    exit 1
fi
line ""
line "\`rds:CreateDBInstance\` é liberado por classe de instância, então cada classe"
line "precisa ser testada separadamente — um \`AccessDenied\` numa classe grande não"
line "significa que o RDS esteja bloqueado."
line ""
line "| Classe | Veredito |"
line "|---|---|"
for instance_class in $RDS_CLASSES; do
    probe="$(aws rds create-db-instance --region "$REGION" \
        --db-instance-identifier "$PROBE_NAME" --db-instance-class "$instance_class" \
        --engine postgres --allocated-storage 20 --master-username probe \
        --master-user-password "$(openssl rand -hex 16)" \
        --db-subnet-group-name "$ABSENT_SUBNET_GROUP" 2>&1)"
    line "| \`$instance_class\` | **$(classify_authorization "$probe")** |"
done

# --- ECR ---------------------------------------------------------------------
section "ECR"
ecr_probe="$(aws ecr create-repository --region "$REGION" \
    --repository-name "$PROBE_NAME" --output json 2>&1)"
if grep -q '"repositoryArn"' <<<"$ecr_probe"; then
    PROBE_ECR_CREATED=1
    line "- \`ecr:CreateRepository\`: **PERMITIDO** — repositório real criado e removido."
    line "- Registry: \`${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com\`"
    if aws ecr delete-repository --region "$REGION" \
        --repository-name "$PROBE_NAME" --force >/dev/null 2>&1; then
        PROBE_ECR_CREATED=""
        line "- Limpeza do probe: OK"
    else
        line "- **Limpeza do probe falhou** — remova \`$PROBE_NAME\` à mão."
    fi
else
    line "- \`ecr:CreateRepository\`: **$(classify_authorization "$ecr_probe")**"
    fence "$ecr_probe"
fi

# --- S3 ----------------------------------------------------------------------
section "S3 (backend de state do Terraform)"
s3_probe="$(aws s3api list-buckets --query 'Buckets[].Name' --output json 2>&1)"
if grep -q '^\[' <<<"$s3_probe"; then
    line "- \`s3:ListAllMyBuckets\`: PERMITIDO — buckets: \`$(jq -c . <<<"$s3_probe")\`"
else
    line "- \`s3:ListAllMyBuckets\`: **$(classify_authorization "$s3_probe")**"
fi

# Listar bucket não prova que o backend funciona: o Terraform precisa de escrita. Este
# probe grava e apaga um objeto vazio no bucket de state.
STATE_BUCKET="${STATE_BUCKET:-$(jq -r '.[0] // empty' <<<"$s3_probe" 2>/dev/null)}"
if [[ -n "$STATE_BUCKET" ]]; then
    # Sem --body o put grava um objeto de zero byte, que é tudo que o probe precisa.
    put_probe="$(aws s3api put-object --bucket "$STATE_BUCKET" \
        --key "verify-lab-probe/$PROBE_NAME" 2>&1)"
    if grep -q '"ETag"' <<<"$put_probe"; then
        line "- \`s3:PutObject\` em \`$STATE_BUCKET\`: **PERMITIDO** — objeto gravado e removido."
        # O bucket de state tem versionamento, então um delete simples deixaria a versão
        # e um delete marker para trás. Remove versão por versão.
        aws s3api list-object-versions --bucket "$STATE_BUCKET" \
            --prefix "verify-lab-probe/$PROBE_NAME" --output json 2>/dev/null \
            | jq -r '[(.Versions // []),(.DeleteMarkers // [])] | flatten | .[] | "\(.Key)\t\(.VersionId)"' \
            | while IFS=$'\t' read -r key version_id; do
                  aws s3api delete-object --bucket "$STATE_BUCKET" \
                      --key "$key" --version-id "$version_id" >/dev/null 2>&1
              done
    else
        line "- \`s3:PutObject\` em \`$STATE_BUCKET\`: **$(classify_authorization "$put_probe")**"
        fence "$put_probe"
    fi
else
    line "- \`s3:PutObject\`: não testado, nenhum bucket encontrado. Defina \`STATE_BUCKET\`."
fi

# --- Implicações -------------------------------------------------------------
section "Implicações para o Terraform"
line "1. A plataforma do spec se sustenta se EKS, RDS e ECR estiverem todos permitidos."
line "2. Role do cluster: \`${EKS_CLUSTER_ROLE:-LabRole}\`. Role dos nós:"
line "   \`${EKS_NODE_ROLE:-LabRole}\`. Resolva as duas por \`data \"aws_iam_roles\"\`"
line "   com \`name_regex\`, porque o prefixo muda a cada reset do lab."
line "3. Prefira a role de node dedicada à \`LabRole\`: só a primeira tem"
line "   \`AmazonEKS_CNI_Policy\` entre as políticas gerenciadas, e sem CNI os nós sobem"
line "   mas não chegam a \`Ready\`. As \`VocLabPolicy*\` da \`LabRole\` são ilegíveis, então"
line "   usar a role dedicada é a escolha que não depende dessa incerteza."
line "4. Filtre as subnets do node group pelas AZs que ofertam \`$NODE_INSTANCE_TYPE\`."
line "5. A classe do RDS é liberada por tamanho — mantenha o default em \`db.t3.micro\`."
line "6. O backend do Terraform usa o bucket de state já existente na conta, desde que o"
line "   probe de \`s3:PutObject\` acima esteja PERMITIDO."

echo "Relatório escrito em $REPORT"
