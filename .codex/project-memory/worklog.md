# Worklog

## 2026-08-15 — Blocking Gitleaks and pre-deploy Trivy scan

- Replaced the non-blocking shared Gitleaks call with a cached, pinned binary
  scan of the complete Git history; findings now fail PR and manual CI runs.
- Pinned the remaining reusable Semgrep, build, and OWASP workflows to the
  reviewed upstream commit instead of `main`.
- Added an official Trivy image scan between GHCR publication and Terraform,
  blocking fixable high/critical CVEs and embedded secrets and retaining the
  report for 14 days.
- A scan of the previous image exposed a baked-in demo private key and vulnerable
  framework dependencies. Removed the key from the image, changed Compose to a
  development-only bind mount, and upgraded the Quarkus LTS platform to
  `3.33.3.1`.
- Verified 393 tests, 88.49% line coverage, every Maven static-analysis gate,
  the production Docker build, and a clean Trivy scan with the same blocking
  policy used by CI. The first GitHub Actions scan of the GHCR image remains
  pending.

## 2026-08-14 — CI/CD quality gate and reports

- Inspected the project's Maven build plugins and confirmed that Spotless,
  Checkstyle, PMD/CPD, SpotBugs, and the JaCoCo 80% line gate are bound to
  `verify`.
- Changed both pull-request validation and the deployment gate to run
  `./mvnw verify -Pitest`, so the Testcontainers integration suite is included.
- Removed the duplicate `quarkus-elytron-security-common` dependency and updated
  the relocated `quarkus-junit5` artifact to `quarkus-junit`.
- Added JaCoCo and JUnit summaries to the deployment run and uploaded all test,
  coverage, and static-analysis reports as a 14-day artifact.
- Local validation passed with 393 tests, zero failures/errors, zero Checkstyle
  or SpotBugs findings, all PMD/CPD checks passing, and 88.49% line coverage.
- Validated all workflow YAML with actionlint 1.7.12 and checked whitespace with
  `git diff --check`; a real GitHub Actions run remains pending.

## 2026-08-14 — SonarQube integration removed

- Removed the disabled SonarQube job and its four secret references from the PR
  workflow, keeping the CI focused on Maven quality checks, Gitleaks, Semgrep,
  and OWASP Dependency-Check.
- Deleted `sonar-project.properties` and reconciled the README and vulnerability
  report so they no longer advertise SonarQube or Tailscale-backed scanning.

## 2026-08-04 — Kubernetes and AWS infrastructure scaffolding

- Added Kubernetes Namespace, ConfigMap, Deployment, Service, HPA, runtime Secret injection, and GHCR image publishing.
- Added Terraform for VPC, EKS, Metrics Server, and Aurora PostgreSQL Serverless v2.
- Added one-time Terraform bootstrap for the S3 state bucket and GitHub OIDC IAM Role.
- Added manual GitHub Actions workflows for Terraform plan/apply and EKS deployment.
- Local validation completed with Terraform 1.15.5 (`init -backend=false`, `validate`), `actionlint`, Kubernetes YAML parsing, and `kubectl --dry-run=client`.
- Real AWS plan/apply remains unverified until the account bootstrap and GitHub Environment `aws` are configured.

## 2026-08-04 — Full CI/CD delivery workflow

- Consolidated `.github/workflows/deploy.yml` into an automatic `main`/manual pipeline that runs Maven build and tests, publishes immutable SHA and `latest` images to GHCR, applies Terraform for Aurora and EKS, creates runtime Kubernetes configuration, applies every application manifest, and waits for the rollout.
- Terraform outputs now provide the EKS cluster name and Aurora writer endpoint to deployment, so duplicate GitHub variables for those values are no longer required.
- Fixed the invalid `configMapRef` key in `k8s/deployment.yaml` and documented the complete pipeline in `README.md`, `k8s/README.md`, and `infra/terraform/README.md`.
- Validation passed with 304 Maven tests and all quality gates, actionlint, Terraform 1.15.5 validation/formatting, kubeconform strict validation of all five manifests, and `git diff --check`.
- A real GitHub Actions run and AWS apply/deploy remain unverified until the `aws` Environment, bootstrap outputs, and secrets are configured in GitHub.

## 2026-08-04 — Safe local Terraform runner

- Added executable `infra/terraform/run-local.sh` with `bootstrap`, `plan`, and confirmation-gated `apply` operations.
- The runner validates AWS authentication, reuses the bootstrap S3 output or `TF_STATE_BUCKET`, and deletes temporary plan files on exit. Database credentials are no longer Terraform inputs.
- Documented AWS SSO and local runner usage in `infra/terraform/README.md`.
- Script validation passed with `bash -n`, ShellCheck, the help-path smoke test, and `git diff --check`; no AWS operation was executed.

## 2026-08-04 — Terraform plan validated in Vocareum

- Configured the S3 backend at `tech-challenge-best-java-group-67/tech-challenge-fase-1/dev/terraform.tfstate` and resolved its stale native lockfile.
- Replaced the EKS community module with native cluster, managed node group, and add-on resources because the module queried the explicitly denied `voclabs` IAM role.
- Reused and validated the lab-provided EKS cluster and node roles; their session-specific ARNs are stored only in ignored `infra/terraform/lab.auto.tfvars`.
- Added `EKS_CLUSTER_ROLE_ARN` and `EKS_NODE_ROLE_ARN` as required GitHub Environment variables for the workflows.
- A real AWS `terraform plan` completed successfully with `28 to add, 0 to change, 0 to destroy`; no apply was executed.
- A subsequent user-run apply created the VPC, EKS cluster, node group, and add-ons, but Aurora creation failed because the configured master username contained a hyphen.
- Changed the ignored local username to the valid alphanumeric value `oficinaadmin` and added an AWS-compatible 1–63 character validation in `infra/terraform/variables.tf`.
- The follow-up real AWS plan succeeded with only the Aurora cluster and writer remaining: `2 to add, 0 to change, 0 to destroy`. Codex did not run apply.
- A second user-run apply exposed an invalid Aurora password containing `@`. Rotated the ignored local password to a 48-character random hexadecimal value without logging or storing it in project memory.
- Added Terraform validation for Aurora PostgreSQL's password length, printable ASCII requirement, and forbidden characters. The subsequent real AWS plan again succeeded with only the two Aurora resources pending; Codex did not run apply.
- A third user-run apply created the Aurora PostgreSQL 16.8 cluster but failed to create its `db.serverless` writer because the Vocareum policy `Pvoclabs2` explicitly denies `rds:CreateDBInstance`.
- AWS inspection confirmed the cluster is `available` with no members, so it cannot serve database connections. Aurora Serverless v2 cannot operate without a writer DB instance, and Serverless v1 no longer accepts new clusters.
- Verified the lab's RDS limitation: `CreateDBCluster` succeeds, but the required `rds:CreateDBInstance` is explicitly denied. `iam:GetPolicy` is also denied, so the policy conditions cannot be inspected, and RDS has no dry-run create operation.
- Replaced Aurora with a PostgreSQL 16 StatefulSet, headless Service, 5 GiB PVC, and static node-local PV; updated application configuration, CI/CD ordering, Terraform, the local runner, and docs.
- Preserved local database credentials in ignored `k8s/postgres.env.local` and removed them from Terraform inputs without logging or storing their values in memory.
- Removed RDS and database-only subnet resources from configuration. A real plan passed with `0 to add, 0 to change, 10 to destroy`; the destructive cleanup was not applied.
- All Kubernetes manifests passed `kubectl apply --dry-run=client --validate=strict` against EKS API discovery. The initial server dry-run could not validate namespace-scoped objects because a dry-run Namespace is not persisted between requests.
- After explicit user approval, generated a saved cleanup plan and programmatically asserted that all ten non-no-op actions were deletes limited to the empty Aurora cluster and database-only network/security resources.
- Applied the exact saved plan successfully: `0 added, 0 changed, 10 destroyed`. A final real AWS plan returned `No changes`.
- Direct AWS verification returned `DBClusterNotFoundFault` for `oficina-mecanica-dev-aurora`, while EKS cluster `oficina-mecanica-dev` remained `ACTIVE`.

## 2026-08-04 — Live Kubernetes deployment completed in EKS

- Applied the namespace, ConfigMap, runtime PostgreSQL/JWT Secret, registry pull Secret, PostgreSQL PV/Service/StatefulSet, API Deployment/Service, and HPA in dependency order.
- Restricted the ignored local PostgreSQL credential file to mode `0600`; no secret values were printed or recorded in project memory.
- PostgreSQL reached `1/1` Ready with its 5 GiB PVC bound to the retained node-local PV, and Flyway successfully created/migrated the `oficina_mecanica` schema.
- Fixed the API pod sandbox failure by defining `runAsUser: 185` alongside the pod-level `runAsGroup: 185`.
- The intended GHCR `latest` image did not exist and the new deployment workflow was not yet present on GitHub. Created the lab ECR repository `tech-challenge-fase-1`, built and pushed tags `4c810048f6c4` and `latest` with digest `sha256:a0af8e6833699b303c6ede39d34bcb1a2aa4bec41002ad2c986c54ec36ea9de2`, and pointed both the live Deployment and the lab manifest default at the immutable tag.
- Corrected the GitHub Actions Secret creation step: `kubectl` cannot combine `--from-env-file` with `--from-file`, so database values now use `--from-literal` and JWT keys remain file-backed.
- The API completed startup and database migration with no restarts. Metrics Server supplied live CPU/memory data; memory utilization above 70% caused the HPA to scale from 2 to its configured maximum of 4, ending with `4/4` API replicas Ready.
- Removed the temporary ECR login from the local Docker configuration after the push.

## 2026-08-04 — Public API exposure through a LoadBalancer Service

- Changed `k8s/service.yaml` from `ClusterIP` to `LoadBalancer` and applied it to the live EKS cluster.
- The EKS service controller provisioned an internet-facing Classic Load Balancer listening on TCP port 8080 and registered the Spot worker successfully as `InService`.
- Verified the complete public path with an unauthenticated request to `/v1/customer`, which reached the Quarkus application and returned the expected `HTTP 401` response.
- Documented how to obtain the generated hostname without persisting it as a stable application URL; deleting/recreating the Service can change the hostname.

## 2026-08-04 — Public OpenAPI and lifecycle-safe CI/CD

- Confirmed inside the live Pod that `/q/openapi` and `/q/swagger-ui` are served on management port 8090, while the same paths correctly return 404 on application port 8080.
- Enabled Swagger UI in the Kubernetes ConfigMap, added public Service port 8090, and rolled all API replicas without downtime; the final Deployment was `4/4` Ready.
- Public verification returned OpenAPI 3.1.0 with 28 paths and `HTTP 200` for Swagger UI.
- Kept the application Load Balancer Kubernetes-managed rather than duplicating it in Terraform. The deploy workflow already creates it by applying the Service.
- Added a confirmation-gated `destroy` operation that removes the LoadBalancer Service before destroying Terraform resources, preventing an orphaned ELB.
- Added GitHub Actions authentication fallback for temporary Vocareum credentials when OIDC is unavailable and documented their required Environment Secret names.
- Added pipeline waits for the ELB hostname and public OpenAPI readiness, then publishes the API/OpenAPI/Swagger URLs in the GitHub job summary.
- Validated both workflows with actionlint 1.7.12, Kubernetes strict client validation, `git diff --check`, a successful live rollout, and public HTTP checks.

## 2026-08-04 — Live AWS runtime teardown completed

- Interpreted the user-requested cleanup as the active/costly runtime stack while preserving the S3 state bucket, ECR repository/images, and local project files for rapid recreation.
- Deleted the Kubernetes `oficina-mecanica-api` LoadBalancer Service first and verified both Kubernetes `NotFound` and AWS `LoadBalancerNotFound`, preventing an orphaned ELB.
- Generated a saved Terraform destroy plan and programmatically asserted exactly 17 changes, all `delete`, limited to the four EKS add-ons, Spot node group, EKS cluster, and Terraform-managed VPC/network resources.
- Applied the exact reviewed plan successfully: `0 added, 0 changed, 17 destroyed`.
- Post-destroy verification confirmed an empty Terraform state plus `ResourceNotFoundException` for EKS, `InvalidVpcID.NotFound` for the VPC, and `LoadBalancerNotFound` for the ELB.
- Confirmed the intentionally preserved ECR repository `tech-challenge-fase-1` and S3 state bucket `tech-challenge-best-java-group-67` remain available in `us-east-1`.
- Removed the temporary saved destroy plan from `/tmp`. Local kubeconfig still contains a stale context and was not modified.

## 2026-08-14 — Automated GHCR release and retention pipeline

- Added manual version input with `v1.0.<github.run_number>` fallback and collision protection that refuses to move an existing Git tag to another commit.
- Added Docker metadata generation for release, full SHA, and `latest` tags; the Kubernetes deployment continues to use the immutable full-SHA reference.
- Added post-deploy Git tag and GitHub Release creation with generated notes through `softprops/action-gh-release`, followed by `actions/delete-package-versions` retention of the three newest GHCR versions.
- Replaced the manifest's live-lab ECR fallback with GHCR and replaced the expiring Kubernetes `GITHUB_TOKEN` pull credential with the required durable `GHCR_PULL_TOKEN` Environment secret.
- Documented the manual release input, Environment configuration, GHCR package Admin access, release ordering, image retention, and safe Terraform destroy workflow.
- Validation passed with actionlint 1.7.12, Maven `verify`, Terraform `fmt -check`/`validate`, kubeconform strict validation of all eight Kubernetes resources, Bash syntax checking, and `git diff --check`.
- A real GitHub Actions publish/deploy/release/cleanup run remains unverified until these untracked infrastructure files are committed and the GitHub Environment is configured.
