#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../infra/terraform/runtime"
test -f terraform.tfvars || { echo 'Create reviewed terraform.tfvars from the example first.' >&2; exit 1; }
terraform init
terraform validate
terraform plan -out=runtime.tfplan
echo 'Plan only. Review AWS pricing, IAM, CIDRs, and support version before any manual apply.'
