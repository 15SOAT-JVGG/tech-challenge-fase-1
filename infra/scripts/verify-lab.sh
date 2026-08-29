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
# AccessDenied aparece antes de qualquer erro de validação. Logo: erro de validação
# ou de recurso inexistente significa que a ação está autorizada.
classify() {
    case "$1" in
        *AccessDenied*|*UnauthorizedOperation*|*"not authorized"*|*"explicit deny"*|*"implicit deny"*)
            echo "BLOQUEADO" ;;
        *ExpiredToken*|*InvalidClientTokenId*|*"security token"*|*NoCredentials*)
            echo "INDETERMINADO" ;;
        *)
            echo "PERMITIDO" ;;
    esac
}

section() { printf '\n## %s\n\n' "$1" >>"$REPORT"; }
line() { printf '%s\n' "$1" >>"$REPORT"; }
fence() { printf '\n```\n%s\n```\n' "$1" >>"$REPORT"; }

: >"$REPORT"
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
while IFS=$'\t' read -r sid az cidr pub; do
    [[ -z "$sid" ]] && continue
    if [[ " $AZS_WITH_TYPE " == *" $az "* ]]; then
        offers="sim"; USABLE_SUBNETS="${USABLE_SUBNETS:+$USABLE_SUBNETS,}$sid"
    else
        offers="**não**"
    fi
    line "| \`$sid\` | $az | \`$cidr\` | $pub | $offers |"
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
    for c in $(jq -r '.clusters[]' <<<"$eks_list"); do
        line "  - \`$c\`: $(aws eks describe-cluster --region "$REGION" --name "$c" \
            --query 'cluster.[status,version,resourcesVpcConfig.vpcId]' --output text 2>/dev/null | tr '\t' ' ')"
    done
else
    line "- \`eks:ListClusters\`: **$(classify "$eks_list")**"
    fence "$eks_list"
fi

eks_probe="$(aws eks create-cluster --region "$REGION" --name "$PROBE_NAME" \
    --role-arn "$LABROLE_ARN" --resources-vpc-config "subnetIds=$ABSENT_SUBNET" 2>&1)"
line "- \`eks:CreateCluster\`: **$(classify "$eks_probe")**"

ng_probe="$(aws eks create-nodegroup --region "$REGION" \
    --cluster-name "$PROBE_NAME" --nodegroup-name probe \
    --node-role "$LABROLE_ARN" --subnets "$ABSENT_SUBNET" 2>&1)"
line "- \`eks:CreateNodegroup\`: **$(classify "$ng_probe")**"

# --- RDS ---------------------------------------------------------------------
section "RDS"
rds_list="$(aws rds describe-db-instances --region "$REGION" \
    --query 'DBInstances[].DBInstanceIdentifier' --output json 2>&1)"
if grep -q '^\[' <<<"$rds_list"; then
    line "- \`rds:DescribeDBInstances\`: PERMITIDO — instâncias existentes: \`$(jq -c . <<<"$rds_list")\`"
else
    line "- \`rds:DescribeDBInstances\`: **$(classify "$rds_list")**"
fi

sg_probe="$(aws rds create-db-subnet-group --region "$REGION" \
    --db-subnet-group-name "$PROBE_NAME" --db-subnet-group-description probe \
    --subnet-ids "$ABSENT_SUBNET" 2>&1)"
line "- \`rds:CreateDBSubnetGroup\`: **$(classify "$sg_probe")**"
line ""
line "\`rds:CreateDBInstance\` é liberado por classe de instância, então cada classe"
line "precisa ser testada separadamente — um \`AccessDenied\` numa classe grande não"
line "significa que o RDS esteja bloqueado."
line ""
line "| Classe | Veredito |"
line "|---|---|"
for cls in $RDS_CLASSES; do
    probe="$(aws rds create-db-instance --region "$REGION" \
        --db-instance-identifier "$PROBE_NAME" --db-instance-class "$cls" \
        --engine postgres --allocated-storage 20 --master-username probe \
        --master-user-password "$(openssl rand -hex 16)" \
        --db-subnet-group-name "$ABSENT_SUBNET_GROUP" 2>&1)"
    line "| \`$cls\` | **$(classify "$probe")** |"
done

# --- ECR ---------------------------------------------------------------------
section "ECR"
ecr_probe="$(aws ecr create-repository --region "$REGION" \
    --repository-name "$PROBE_NAME" --output json 2>&1)"
if grep -q '"repositoryArn"' <<<"$ecr_probe"; then
    line "- \`ecr:CreateRepository\`: **PERMITIDO** — repositório real criado e removido."
    line "- Registry: \`${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com\`"
    if aws ecr delete-repository --region "$REGION" \
        --repository-name "$PROBE_NAME" --force >/dev/null 2>&1; then
        line "- Limpeza do probe: OK"
    else
        line "- **Limpeza do probe falhou** — remova \`$PROBE_NAME\` à mão."
    fi
else
    line "- \`ecr:CreateRepository\`: **$(classify "$ecr_probe")**"
    fence "$ecr_probe"
fi

# --- S3 ----------------------------------------------------------------------
section "S3 (backend de state do Terraform)"
s3_probe="$(aws s3api list-buckets --query 'Buckets[].Name' --output json 2>&1)"
if grep -q '^\[' <<<"$s3_probe"; then
    line "- \`s3:ListAllMyBuckets\`: PERMITIDO — buckets: \`$(jq -c . <<<"$s3_probe")\`"
else
    line "- \`s3:ListAllMyBuckets\`: **$(classify "$s3_probe")**"
fi

# --- Implicações -------------------------------------------------------------
section "Implicações para o Terraform"
line "1. A plataforma do spec se sustenta se EKS, RDS e ECR estiverem todos permitidos."
line "2. Role do cluster: \`${EKS_CLUSTER_ROLE:-LabRole}\`. Role dos nós:"
line "   \`${EKS_NODE_ROLE:-LabRole}\`. Resolva as duas por \`data \"aws_iam_roles\"\`"
line "   com \`name_regex\`, porque o prefixo muda a cada reset do lab."
line "3. A \`LabRole\` **não** serve como role de nó: falta \`AmazonEKS_CNI_Policy\`,"
line "   e sem ela os nós sobem mas nunca chegam a \`Ready\`."
line "4. Filtre as subnets do node group pelas AZs que ofertam \`$NODE_INSTANCE_TYPE\`."
line "5. A classe do RDS é liberada por tamanho — mantenha o default em \`db.t3.micro\`."
line "6. O backend do Terraform usa o bucket de state já existente na conta."

echo "Relatório escrito em $REPORT"
