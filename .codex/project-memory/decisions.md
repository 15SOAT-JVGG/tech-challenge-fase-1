# Architecture decisions

## 2026-08-14 — Build and deploy require the full Maven quality gate

Status: accepted

- Pull requests and deployments execute `mvn verify -Pitest`, covering unit and
  Testcontainers integration tests before an image can be published.
- The Maven `verify` lifecycle blocks on Spotless, Checkstyle, PMD/CPD,
  SpotBugs, and a JaCoCo line-coverage threshold of 80%.
- The deploy workflow publishes JUnit and JaCoCo summaries and retains the raw
  reports as a GitHub Actions artifact for 14 days.
- The existing PR workflow continues to add Gitleaks, Semgrep, and OWASP
  Dependency-Check. SonarQube is intentionally omitted to keep CI self-contained
  and avoid the external homelab/Tailscale dependency.

Rationale: infrastructure and application deployment must not begin when code
style, static analysis, automated tests, or coverage fail.

References: `pom.xml`, `.github/workflows/ci.yml`,
`.github/workflows/deploy.yml`, `README.md`.

Last verified locally with 393 passing tests, zero static-analysis violations,
and 88.49% line coverage on 2026-08-14.

## 2026-08-15 — Security gates are local or revision-pinned

Status: accepted

- Gitleaks runs directly in the PR/manual CI with complete history and blocks on
  findings; the shared implementation was not used because it temporarily
  ignored scan failures.
- Semgrep, the Quarkus build, and OWASP Dependency-Check remain reusable but are
  pinned to reviewed commit `ecbbbeba3ef6e46082a374d5d918ad40140c144e` instead
  of the mutable `main` branch.
- The immutable GHCR image is scanned by Trivy for high/critical fixable CVEs and
  embedded secrets before Terraform can create AWS resources. The report is
  retained for 14 days.
- JWT private keys are never baked into the runtime image: Docker Compose mounts
  the development pair and Kubernetes mounts the GitHub-provided Secret.

Rationale: security checks must block delivery and shared workflow changes must
not silently change this repository's pipeline.

References: `.github/workflows/ci.yml`, `.github/workflows/deploy.yml`,
`docs/RELATORIO-VULNERABILIDADES.md`.

Last verified locally with workflow static validation, 393 passing tests,
88.49% line coverage, and a clean Trivy scan of the resulting image on
2026-08-15. The first GitHub Actions scan of the GHCR image remains pending.

## 2026-08-04 — AWS is the primary deployment target

Status: accepted

- The application targets Amazon EKS in `us-east-1`; the previous homelab/Tailscale deployment path is superseded.
- AWS access from GitHub Actions prefers OIDC through the GitHub Environment `aws`. In Vocareum, where IAM/OIDC setup is blocked, the workflows fall back to the lab's temporary access key, secret key, and session token stored as Environment Secrets; kubeconfig is always generated during the job and never stored.
- Persistent secret values remain in GitHub Environment Secrets and are materialized as a Kubernetes Secret only during deployment.
- Terraform remote state uses a private, versioned S3 bucket with the S3 native lockfile.

Rationale: the user selected the lowest-cost practical AWS region/profile and EKS as the deployment focus.

References: `infra/terraform/`, `.github/workflows/terraform.yml`, `.github/workflows/deploy.yml`, `k8s/README.md`.

## 2026-08-04 — Cost-optimized development infrastructure

Status: accepted

- EKS uses Kubernetes 1.36, one initial Spot worker (`t3.medium` or `t3a.medium`), and Metrics Server as an EKS community add-on.
- Workers run in public subnets to avoid NAT Gateway hourly/data-processing charges; security groups do not open inbound internet traffic.
- The original Aurora Serverless v2 design is superseded for the Vocareum lab because `rds:CreateDBInstance` is explicitly denied.
- PostgreSQL runs as a single-replica Kubernetes StatefulSet for the lab; production should use a managed database or properly configured CSI storage.
- This is a development/demo profile, not a high-availability production baseline. EKS control-plane and public IPv4 charges remain.

Last verified against code: 2026-08-04.

## 2026-08-04 — PostgreSQL StatefulSet fallback for Vocareum

Status: accepted

- PostgreSQL 16 is deployed by `k8s/postgres.yaml` as a one-replica StatefulSet with a headless Service, a 5 GiB PVC, and a static `hostPath` PV.
- The application uses `postgres.oficina-mecanica.svc.cluster.local`; database credentials remain GitHub Environment secrets materialized into `oficina-mecanica-secrets`.
- The EKS cluster exposes a legacy `gp2` StorageClass but has no EBS CSI driver, and its EFS CSI object has no running driver pods. Node-local storage is therefore used only for the lab.
- Data survives pod restarts on the same node but is lost if the Spot node is replaced. The PV reclaim policy is `Retain`.
- Terraform no longer manages RDS or database-only subnets. The empty Aurora cluster and nine associated network/security resources were deleted by the reviewed cleanup plan.

Rationale: the lab identity can create an Aurora cluster but has an explicit deny on the mandatory `rds:CreateDBInstance` writer operation; the user accepted evaluating the StatefulSet fallback.

Last verified with AWS, EKS, Terraform apply/final plan, and Kubernetes strict client dry-run: 2026-08-04.

## 2026-08-04 — Vocareum EKS reuses lab-managed IAM roles

Status: accepted

- The main Terraform uses native `aws_eks_*` resources instead of the EKS community module, avoiding the module's `iam:GetRole` call against the explicitly denied `voclabs` role.
- EKS cluster and node IAM roles are required inputs (`eks_cluster_role_arn` and `eks_node_role_arn`) and are never created by the main stack.
- Local lab-specific ARNs live in ignored `*.tfvars` files; GitHub Actions receives the equivalent values from `EKS_CLUSTER_ROLE_ARN` and `EKS_NODE_ROLE_ARN` Environment variables.
- GitHub OIDC bootstrap still requires IAM administration and therefore remains unavailable in the current lab unless an administrator preconfigures it.

Rationale: the Vocareum account explicitly denies IAM access but provides service-trusted EKS cluster and node roles that can be passed to EKS.

Last verified with an AWS plan in account `861757207687`: 2026-08-04.

## 2026-08-04 — ECR is the live-lab registry fallback

Status: superseded on 2026-08-14 by the GHCR release pipeline

- The repository CI/CD design continues to publish immutable commit tags and `latest` to GHCR.
- The workflow is currently only in the local `feature/infra` branch, and no GHCR package/tag existed during the live deployment.
- To complete the authorized lab deployment without storing another long-lived GitHub token, the now-destroyed live Deployment used the AWS ECR image `tech-challenge-fase-1:4c810048f6c4`.
- The ECR repository was created directly in the lab account and is not currently managed by Terraform. It should either be imported/declared if ECR becomes permanent or removed after GHCR CI/CD is published and verified.

Rationale: the local GitHub OAuth token lacked package-publish scope, while the lab identity allowed ECR creation and EKS worker nodes could pull from ECR using their AWS role.

Last verified: the ECR repository remains available after the EKS teardown on 2026-08-04; no live Deployment currently exists.

## 2026-08-14 — GHCR, Git tags, and releases are managed by CI/CD

Status: accepted

- Every push to `main` and every manual deployment publishes one GHCR image with release, full commit SHA, and `latest` tags.
- Automatic versions use `v1.0.<github.run_number>`; manual runs may supply a `vX.Y.Z` version. An existing version may only be reused for the same commit.
- Docker metadata, GitHub Release creation, and package retention use maintained GitHub Actions rather than custom API scripts. The Git tag and release are created only after the EKS rollout and public OpenAPI smoke test succeed.
- GHCR retains the three newest container versions. The repository must have Admin access to the package for cleanup with `GITHUB_TOKEN`.
- Private image pulls use the durable `GHCR_PULL_TOKEN` Environment secret and optional `GHCR_USERNAME` variable. The job-scoped `GITHUB_TOKEN` is not persisted in Kubernetes because it expires after the workflow.
- The lab ECR repository is no longer referenced by Kubernetes manifests and remains external to Terraform until it is manually removed or adopted.

Rationale: the user selected GHCR and requested automatic tags/releases, use of existing Actions whenever possible, and retention of only the three latest images.

References: `.github/workflows/deploy.yml`, `k8s/deployment.yaml`, `k8s/README.md`.

Last verified locally with actionlint 1.7.12, Maven verify, Terraform validate, and kubeconform 0.7.0 on 2026-08-14. A real GitHub Actions run remains pending.

## 2026-08-04 — Direct public exposure with a LoadBalancer Service

Status: accepted

- The API is exposed directly by the Kubernetes Service in `k8s/service.yaml` using `type: LoadBalancer`.
- API Gateway and Ingress are intentionally omitted for the lab because the project currently exposes a single HTTP service and the user selected the simplest option.
- Because AWS Load Balancer Controller is not installed, the EKS service controller provisioned an internet-facing Classic Load Balancer with TCP listeners on port 8080 for the API and 8090 for OpenAPI/Swagger/management.
- OpenAPI is served by Quarkus on the management interface because `quarkus.management.enabled=true`; the lab Service intentionally exposes that port and `SWAGGER_UI_ENABLED=true` keeps the interactive UI available.
- This lab endpoint has no TLS, custom domain, WAF, API quotas, or gateway-level authentication. Port 8090 also exposes other Quarkus management endpoints. Those concerns require a later ALB/Ingress or API Gateway design.
- The AWS Load Balancer incurs charges while the Service exists, and its generated hostname can change if the Service is deleted and recreated.

Rationale: direct `LoadBalancer` exposure requires the fewest moving parts and worked with the current Vocareum permissions without adding IAM roles or Kubernetes controllers.

References: `k8s/service.yaml`, `k8s/README.md`.

Last verified before teardown with Kubernetes, Classic ELB listeners/instance health, public OpenAPI 3.1 output, and Swagger UI HTTP 200. The live endpoint was removed on 2026-08-04; this remains the desired configuration for recreation.

## 2026-08-04 — Kubernetes owns Load Balancer lifecycle

Status: accepted

- Terraform creates VPC and EKS, but does not declare a duplicate `aws_elb` resource for the application.
- The deployment pipeline applies `k8s/service.yaml`; the EKS service controller then provisions the AWS Load Balancer and reports its hostname in Service status.
- The manual Terraform workflow supports `destroy` only with `confirm_destroy=DESTROY`. It first deletes the Kubernetes LoadBalancer Service with finalizer waiting, then plans and applies Terraform destruction.
- OIDC remains preferred, with temporary Vocareum credentials used only when `AWS_ROLE_ARN` is absent.
- The main destroy does not remove the bootstrap/state S3 bucket or the manually created ECR repository.

Rationale: keeping one owner for the ELB avoids Terraform/Kubernetes lifecycle conflicts, while pre-destroy cleanup prevents an orphaned chargeable Load Balancer.

References: `.github/workflows/deploy.yml`, `.github/workflows/terraform.yml`, `k8s/service.yaml`, `infra/terraform/README.md`.

Last verified by an actual cleanup: the Kubernetes Service finalizer removed the ELB before Terraform destroyed EKS/VPC on 2026-08-04.
