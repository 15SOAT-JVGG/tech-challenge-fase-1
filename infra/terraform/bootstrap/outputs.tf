output "state_bucket_name" {
  description = "Value for the GitHub variable TF_STATE_BUCKET."
  value       = aws_s3_bucket.terraform_state.id
}

output "github_actions_role_arn" {
  description = "Value for the GitHub variable AWS_ROLE_ARN."
  value       = aws_iam_role.github_actions.arn
}

output "aws_account_id" {
  description = "AWS account initialized by the bootstrap."
  value       = data.aws_caller_identity.current.account_id
}
