# Rede, banco e API Gateway pertencem à stack da plataforma (infra/terraform). Ler o state
# dela, em vez de repetir data sources, é o que garante que a função fala com o mesmo banco
# e pendura a rota na mesma API que o cluster usa.
data "terraform_remote_state" "platform" {
  backend = "s3"

  config = {
    bucket = var.platform_state_bucket
    key    = var.platform_state_key
    region = var.aws_region
  }
}

# O Learner Lab não deixa criar role IAM (ADR-0001), então a função assume a LabRole. Ela
# precisa confiar em lambda.amazonaws.com e permitir criar ENI na VPC; o relatório de
# infra/scripts/verify-lab.sh lista as confianças dela antes do primeiro apply.
data "aws_iam_role" "lab" {
  name = "LabRole"
}

locals {
  function_name = "${var.project_name}-customer-auth"
  platform      = data.terraform_remote_state.platform.outputs
  postgres_port = local.platform.database_port
}

resource "aws_security_group" "function" {
  name        = local.function_name
  description = "Lambda de autenticacao por CPF; sai apenas para o Postgres"
  vpc_id      = local.platform.vpc_id
}

# A única saída é o banco. A função não chama nada na internet, e por isso dispensa NAT.
resource "aws_vpc_security_group_egress_rule" "function_to_database" {
  security_group_id            = aws_security_group.function.id
  description                  = "Postgres da oficina"
  referenced_security_group_id = local.platform.database_security_group_id
  ip_protocol                  = "tcp"
  from_port                    = local.postgres_port
  to_port                      = local.postgres_port
}

resource "aws_vpc_security_group_ingress_rule" "database_from_function" {
  security_group_id            = local.platform.database_security_group_id
  description                  = "Postgres a partir da Lambda de autenticacao"
  referenced_security_group_id = aws_security_group.function.id
  ip_protocol                  = "tcp"
  from_port                    = local.postgres_port
  to_port                      = local.postgres_port
}

# Criado antes da função para que a retenção valha desde a primeira invocação; se a Lambda
# criasse o grupo sozinha, ele nasceria sem expiração.
resource "aws_cloudwatch_log_group" "function" {
  name              = "/aws/lambda/${local.function_name}"
  retention_in_days = var.log_retention_days
}

resource "aws_lambda_function" "customer_auth" {
  function_name = local.function_name
  description   = "Valida o CPF, consulta o cliente no RDS e emite o JWT RS256 aceito pela API"
  role          = data.aws_iam_role.lab.arn

  runtime       = "java21"
  architectures = ["x86_64"]
  handler       = "br.com.fiap.postech.soat16.customerauth.adapter.in.apigateway.CustomerAuthHandler::handleRequest"

  filename         = var.package_path
  source_code_hash = filebase64sha256(var.package_path)

  memory_size = var.memory_size
  # Cobre o cold start da JVM somado ao handshake TLS com o RDS; o API Gateway desiste em 29s.
  timeout = 15

  vpc_config {
    subnet_ids         = local.platform.node_subnet_ids
    security_group_ids = [aws_security_group.function.id]
  }

  environment {
    variables = {
      DB_HOST                = local.platform.database_host
      DB_PORT                = tostring(local.postgres_port)
      DB_NAME                = local.platform.database_name
      DB_USERNAME            = local.platform.database_username
      DB_PASSWORD            = local.platform.database_password
      DB_SSLMODE             = "require"
      JWT_PRIVATE_KEY_B64    = var.jwt_private_key_b64 # gitleaks:allow — referência a variável sensível, não o valor
      JWT_ISSUER             = var.jwt_issuer
      JWT_EXPIRATION_MINUTES = tostring(var.token_lifetime_minutes)
    }
  }

  logging_config {
    log_format = "Text"
    log_group  = aws_cloudwatch_log_group.function.name
  }

  depends_on = [aws_cloudwatch_log_group.function]

  lifecycle {
    precondition {
      condition     = strcontains(data.aws_iam_role.lab.assume_role_policy, "lambda.amazonaws.com")
      error_message = "A LabRole desta conta não confia em lambda.amazonaws.com; a função não teria como assumi-la."
    }
  }
}

resource "aws_apigatewayv2_integration" "customer_auth" {
  api_id                 = local.platform.api_gateway_id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.customer_auth.invoke_arn
  payload_format_version = "2.0"
  timeout_milliseconds   = 29000
}

resource "aws_apigatewayv2_route" "customer_auth" {
  api_id    = local.platform.api_gateway_id
  route_key = "POST /v1/auth/customer"
  target    = "integrations/${aws_apigatewayv2_integration.customer_auth.id}"
}

resource "aws_lambda_permission" "api_gateway" {
  statement_id  = "AllowInvokeFromApiGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.customer_auth.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${local.platform.api_gateway_execution_arn}/*/*/v1/auth/customer"
}
