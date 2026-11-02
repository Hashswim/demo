#!/usr/bin/env bash
set -euo pipefail
# Deliberately read-only: this script never destroys AWS infrastructure.
: "${NAMESPACE:=staffing}"
kubectl -n "$NAMESPACE" get jobs,pods,pvc
printf '%s\n' 'Before deleting: stop new analysis, drain/recover jobs, run backup Job, verify S3 object and full restore, inventory retained EBS/snapshots/IPs/logs.'
printf '%s\n' 'Persistent S3/ECR stack must stay. Review a separate Terraform destroy plan manually. Budget alerts do not stop billing.'
