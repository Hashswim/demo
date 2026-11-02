variable "region" { type = string }
variable "name" {
  type    = string
  default = "staffing"
}
variable "kubernetes_version" {
  type        = string
  description = "Choose a currently STANDARD-supported EKS version after checking regional availability. No hardcoded stale default."
}
variable "instance_type" {
  type    = string
  default = "m6a.xlarge"
}
variable "operator_cidrs" {
  type        = list(string)
  description = "Operator public IP /32 CIDRs for EKS API. Never 0.0.0.0/0."
  validation {
    condition     = length(var.operator_cidrs) > 0 && alltrue([for c in var.operator_cidrs : can(cidrhost(c, 0)) && !endswith(c, "/0")])
    error_message = "Provide specific valid operator CIDRs; /0 is forbidden."
  }
}
variable "user_cidrs" {
  type        = list(string)
  description = "Allowed HTTPS client CIDRs. Never 0.0.0.0/0."
  validation {
    condition     = length(var.user_cidrs) > 0 && alltrue([for c in var.user_cidrs : can(cidrhost(c, 0)) && !endswith(c, "/0")])
    error_message = "Provide specific valid user CIDRs; /0 is forbidden."
  }
}
variable "artifact_bucket_arn" { type = string }
variable "budget_email" { type = string }
variable "budget_usd" {
  type    = number
  default = 100
  validation {
    condition     = var.budget_usd > 0 && var.budget_usd <= 100
    error_message = "MVP budget must be <= $100. AWS Budgets is not an immediate spending cap."
  }
}
variable "eks_addon_ebs_version" {
  type        = string
  description = "Explicit EBS CSI addon version compatible with selected EKS version."
}
