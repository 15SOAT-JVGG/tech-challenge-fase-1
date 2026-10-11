# Porta de entrada única da API. O HTTP API, e não o REST API, porque cobre o que a fase
# pede — rota por método e caminho, integração Lambda e proxy HTTP, throttling e log de
# acesso — com latência e custo menores, e sem o deploy explícito de estágio do REST API.
#
# A rota POST /v1/auth/customer, que leva à função de autenticação por CPF, não é criada
# aqui: ela pertence à stack da função (lambda/customer-auth/terraform), que só precisa
# do id desta API. O resto do tráfego segue para o cluster.
resource "aws_apigatewayv2_api" "main" {
  name          = var.project_name
  protocol_type = "HTTP"
  description   = "Entrada da API da oficina: autenticacao por CPF e proxy para o cluster"
}

resource "aws_cloudwatch_log_group" "api_gateway_access" {
  name              = "/aws/apigateway/${var.project_name}"
  retention_in_days = var.log_retention_days
}

# O estágio $default publica cada mudança de rota sem um deploy à parte. O log de acesso em
# JSON é o que dá latência por rota, status e o requestId que correlaciona a chamada com o
# log da função e da aplicação.
resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.main.id
  name        = "$default"
  auto_deploy = true

  default_route_settings {
    throttling_burst_limit = var.api_throttling_burst_limit
    throttling_rate_limit  = var.api_throttling_rate_limit
  }

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.api_gateway_access.arn
    format = jsonencode({
      requestId          = "$context.requestId"
      ip                 = "$context.identity.sourceIp"
      httpMethod         = "$context.httpMethod"
      routeKey           = "$context.routeKey"
      path               = "$context.path"
      status             = "$context.status"
      responseLength     = "$context.responseLength"
      latencyMs          = "$context.responseLatency"
      integrationLatency = "$context.integrationLatency"
      integrationStatus  = "$context.integrationStatus"
      integrationError   = "$context.integration.error"
      requestTime        = "$context.requestTime"
    })
  }
}

# O ELB nasce do Service do Kubernetes, não do Terraform, então o endereço só existe depois
# do primeiro deploy da aplicação. Sem ele a API sobe só com a rota de autenticação; com
# ele, um segundo apply liga o proxy (ver infra/README.md).
resource "aws_apigatewayv2_integration" "cluster" {
  count = var.app_upstream_url == "" ? 0 : 1

  api_id             = aws_apigatewayv2_api.main.id
  integration_type   = "HTTP_PROXY"
  integration_method = "ANY"
  integration_uri    = "${trimsuffix(var.app_upstream_url, "/")}/{proxy}"

  # O cluster responde em até 29s, o teto do HTTP API; acima disso o gateway devolve 504
  # antes da aplicação, e o access log registra integrationError.
  timeout_milliseconds = 29000
}

resource "aws_apigatewayv2_route" "cluster" {
  count = var.app_upstream_url == "" ? 0 : 1

  api_id    = aws_apigatewayv2_api.main.id
  route_key = "ANY /{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.cluster[0].id}"
}
