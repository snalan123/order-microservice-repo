# CI: Build & Push Docker Image to AWS ECR via OIDC (GitLab CI)

This pipeline builds a Docker image with **Docker-in-Docker (DinD)** and **pushes** it to **Amazon ECR** using **OIDC** (no long-lived AWS keys).

---

## Pipeline file

The pipeline lives in `.gitlab-ci.yml` and contains a single job: `build-and-push`.

```yaml
image: docker:24.0
services:
  - name: docker:24.0-dind
    command: ["--tls=false"]

stages: [build-and-push]

variables:
  DOCKER_HOST: tcp://docker:2375
  DOCKER_TLS_CERTDIR: ""
  DOCKER_DRIVER: overlay2

  AWS_ACCOUNT_ID: "831955480324"
  AWS_REGION: "ap-south-1"
  AWS_ROLE_ARN: "arn:aws:iam::831955480324:role/gitlab-runner-ecr-role"
  ECR_REPOSITORY: "order-microservice"
  IMAGE_TAG: "3.0.0"

build-and-push:
  stage: build-and-push
  interruptible: true
  retry: 1

  # Request an OIDC token with audience = sts.amazonaws.com
  id_tokens:
    AWS_ID_TOKEN:
      aud: sts.amazonaws.com

  before_script:
    - |
      set -xeu
      echo "Commit: $CI_COMMIT_SHA on $CI_COMMIT_REF_NAME"
      apk add --no-cache aws-cli jq curl
      which aws || true
      aws --version || true
      docker --version || true

      # Configure AWS OIDC (web identity)
      echo "$AWS_ID_TOKEN" > /tmp/web_identity_token
      export AWS_WEB_IDENTITY_TOKEN_FILE=/tmp/web_identity_token
      export AWS_ROLE_ARN="$AWS_ROLE_ARN"
      export AWS_DEFAULT_REGION="$AWS_REGION"
      export AWS_ROLE_SESSION_NAME="gitlab-${CI_PIPELINE_ID}"

      # ECR coordinates
      ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
      IMAGE_NAME="${ECR_REGISTRY}/${ECR_REPOSITORY}"
      export ECR_REGISTRY IMAGE_NAME

      echo "TARGET IMAGE → ${IMAGE_NAME}:${IMAGE_TAG}"
      docker info

  script:
    - aws sts get-caller-identity
    - aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$ECR_REGISTRY"
    - |
      aws ecr describe-repositories --repository-names "${ECR_REPOSITORY}" >/dev/null 2>&1 \
      || aws ecr create-repository --repository-name "${ECR_REPOSITORY}"
    - docker pull "${IMAGE_NAME}:latest" || true
    - docker build --pull --cache-from "${IMAGE_NAME}:latest" -t order-microservice:latest .
    - docker tag order-microservice:latest "${IMAGE_NAME}:${IMAGE_TAG}"
    - docker push "${IMAGE_NAME}:${IMAGE_TAG}"

  rules:
    - if: '$CI_COMMIT_BRANCH == "master"'
```

---

##  What this pipeline does

1. **Authenticates to AWS using OIDC**
   The job requests an OIDC token (audience `sts.amazonaws.com`) and exports it to `AWS_WEB_IDENTITY_TOKEN_FILE`. AWS STS exchanges this token for temporary credentials tied to the IAM role in `AWS_ROLE_ARN`.

2. **Authenticates Docker to ECR**
   Uses `aws ecr get-login-password` to log in to the target ECR registry.

3. **Builds, tags, and pushes** a Docker image

   * Builds from the repository’s `Dockerfile`.
   * Tags the image as `${IMAGE_TAG}`.
   * Pushes to `/${ECR_REPOSITORY}` in the configured account/region.

4. **Runs only on `master`** (per the `rules:` clause).

---

##  Prerequisites

1. **IAM Identity Provider (OIDC)**

   * Create an **OIDC provider** in AWS IAM for **`gitlab.com`** with audience `sts.amazonaws.com`.

2. **IAM Role for GitLab (Web Identity)**

   * Create an IAM Role (e.g., `gitlab-runner-ecr-role`) with:

     * **Trust policy** allowing `sts:AssumeRoleWithWebIdentity` from `gitlab.com` with:

       * `gitlab.com:aud = sts.amazonaws.com`
       * `gitlab.com:sub` scoping to your **group/project/branch** (e.g., `project_path:mygroup/myrepo:ref_type:branch:ref:*`).
     * **Permissions policy** that allows:

       * `ecr:GetAuthorizationToken` (Resource `*`)
       * Push/Pull on your repo:

         * `ecr:BatchCheckLayerAvailability`
         * `ecr:CompleteLayerUpload`
         * `ecr:InitiateLayerUpload`
         * `ecr:UploadLayerPart`
         * `ecr:PutImage`
         * `ecr:BatchGetImage`
         * `ecr:GetDownloadUrlForLayer`
         * `ecr:DescribeRepositories`
       * (Optional) `ecr:CreateRepository` on your repo ARN to auto-create the repo.

3. **ECR Repository**

   * Create ECR repo `order-microservice` in `ap-south-1`, or let the job create it (if permitted).

4. **GitLab CI/CD Variables (masked/protected)**

   * `AWS_ACCOUNT_ID` (e.g., `831955480324`)
   * `AWS_REGION` (e.g., `ap-south-1`)
   * `AWS_ROLE_ARN` (e.g., `arn:aws:iam::831955480324:role/gitlab-runner-ecr-role`)
   * `ECR_REPOSITORY` (e.g., `order-microservice`)
   * `IMAGE_TAG` (e.g., `3.0.0`, or switch to `$CI_COMMIT_SHA`)

> **Note:** No static AWS Access Keys are used or stored. The job assumes the IAM role via OIDC.

---

## How to run

Push to the **`master`** branch. The job:

* Builds the image as `order-microservice:latest`
* Tags it as `${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPOSITORY}:${IMAGE_TAG}`
* Pushes the tag to ECR

---

## Common customizations

* **Tag strategy**
  Replace `IMAGE_TAG: "3.0.0"` with:

  * `IMAGE_TAG: "$CI_COMMIT_SHA"` for immutable images
  * or tag on Git tags only: add a `rules:` entry like `if: '$CI_COMMIT_TAG'`.

* **Run on more branches**
  Adjust `rules:` (e.g., run on all branches, but push `latest` only on `master`).

* **Multi-stage pipeline**
  Split into `test`, `build`, `deploy` stages. Use `maven:3.9-eclipse-temurin-21` for unit tests in the `test` stage.

---

## Troubleshooting

* **`CI_JOB_JWT_V2: parameter not set`**
  You’re using `id_tokens`, so consume **`$AWS_ID_TOKEN`** (already done in this file).

* **`Cannot perform an interactive login from a non TTY device`**
  Ensure the ECR login command is **one line**:
  `aws ecr get-login-password ... | docker login ...`

* **`error: externally-managed-environment`** from `pip`
  Don’t use `pip`. We install **`aws-cli`** via `apk` (`apk add --no-cache aws-cli`).

* **DinD connectivity issues**
  Keep:

  * `services: docker:…-dind`
  * `DOCKER_HOST=tcp://docker:2375`
  * `DOCKER_TLS_CERTDIR=""`
  * Some runners also require `privileged: true`.

* **Role not assumable / AccessDenied**
  Re-check IAM **trust policy** conditions:

  * `gitlab.com:aud = sts.amazonaws.com`
  * `gitlab.com:sub = project_path:<group>/<project>:ref_type:branch:ref:*` (or your exact branch)

---

## Security notes

* OIDC eliminates long-lived AWS keys: short-lived STS creds only.
* Scope the **trust policy** to your **exact project** and (optionally) **branch**.
* Use **least-privilege** ECR permissions and restrict the repository ARN.
* Protect variables and the `master` branch in GitLab.

---

##  Quick sanity checklist

* [ ] IAM Identity Provider `gitlab.com` (audience `sts.amazonaws.com`)
* [ ] IAM Role trust policy scoped to `project_path:…` and `ref_type:branch`
* [ ] Role permissions allow ECR push/pull (and create if desired)
* [ ] GitLab variables set (`AWS_*`, repo, region, tag)
* [ ] Runner can use DinD (and `docker info` succeeds)

That’s it! Commit the pipeline and push to `master` to build and publish your image to ECR.
