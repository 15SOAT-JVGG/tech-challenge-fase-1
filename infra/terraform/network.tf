module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "6.6.1"

  name = "${local.name}-vpc"
  cidr = var.vpc_cidr

  azs            = local.azs
  public_subnets = ["10.0.0.0/24", "10.0.1.0/24"]

  enable_dns_hostnames = true
  enable_dns_support   = true

  # Cost profile: nodes use public IPv4 for outbound traffic, avoiding NAT Gateway.
  map_public_ip_on_launch = true
  enable_nat_gateway      = false

  public_subnet_tags = {
    "kubernetes.io/role/elb"              = "1"
    "kubernetes.io/cluster/${local.name}" = "shared"
  }

  tags = local.common_tags
}
