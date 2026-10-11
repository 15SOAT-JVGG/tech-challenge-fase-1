output "function_name" {
  description = "Nome da função, para logs e invocação direta."
  value       = aws_lambda_function.customer_auth.function_name
}

output "customer_auth_url" {
  description = "Endpoint público de autenticação por CPF."
  value       = "${trimsuffix(local.platform.api_gateway_url, "/")}/v1/auth/customer"
}
