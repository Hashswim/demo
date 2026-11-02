{{- define "staffing.name" -}}{{ .Release.Name }}{{- end -}}
{{- define "staffing.secret" -}}{{ required "secrets.existingSecret is required; never put passwords in values.yaml" .Values.secrets.existingSecret }}{{- end -}}
