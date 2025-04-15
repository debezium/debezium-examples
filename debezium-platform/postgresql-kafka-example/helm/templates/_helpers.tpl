{{/*
Get the database secret name.
*/}}
{{- define "debezium-platform.secretName" -}}
{{- if .Values.database.enabled -}}
    {{ include "database.secretName" .Subcharts.database }}
{{- else -}}
    {{- required "A valid .Values.database.auth.existingSecret entry required!" .Values.database.auth.existingSecret -}}
{{- end -}}
{{- end -}}

{{/*
Get the offset config map name.
*/}}
{{- define "debezium-platform.offsetConfigMapName" -}}
{{- if empty .Values.conductor.offset.existingConfigMap -}}
    {{- printf "%s-%s" .Chart.Name "offsets" -}}
{{- else -}}
    {{- .Values.conductor.offset.existingConfigMap -}}
{{- end -}}
{{- end -}}

{{/*
Generates offset envs.
*/}}
{{- define "debezium-platform.offsetConfig" -}}
{{- if not .Values.offset.reusePlatformDatabase -}}
- name: OFFSET_JDBC_URL
  value: jdbc:postgresql://{{ .Values.offset.database.host }}:{{ .Values.offset.database.port }}/{{ .Values.offset.database.name }}
- name: OFFSET_JDBC_USERNAME
{{- if .Values.offset.database.auth.existingSecret }}
  valueFrom:
    secretKeyRef:
      name: {{ .Values.offset.database.auth.existingSecret }}
      key: username
{{- else }}
  value: {{ .Values.offset.database.username }}
{{- end }}
- name: OFFSET_JDBC_PASSWORD
{{- if .Values.offset.database.auth.existingSecret }}
  valueFrom:
    secretKeyRef:
      name: {{ .Values.offset.database.auth.existingSecret }}
      key: password
{{- else }}
  value: {{ .Values.offset.database.password }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Generates schema history envs.
*/}}
{{- define "debezium-platform.schemaHistoryConfig" -}}
{{- if not .Values.schemaHistory.reusePlatformDatabase -}}
- name: SCHEMA_HISTORY_JDBC_URL
  value: jdbc:postgresql://{{ .Values.schemaHistory.database.host }}:{{ .Values.schemaHistory.database.port }}/{{ .Values.schemaHistory.database.name }}
- name: SCHEMA_HISTORY_JDBC_USERNAME
{{- if .Values.schemaHistory.database.auth.existingSecret }}
  valueFrom:
    secretKeyRef:
      name: {{ .Values.schemaHistory.database.auth.existingSecret }}
      key: username
{{- else }}
  value: {{ .Values.schemaHistory.database.username }}
{{- end }}
- name: SCHEMA_HISTORY_JDBC_PASSWORD
{{- if .Values.schemaHistory.database.auth.existingSecret }}
  valueFrom:
    secretKeyRef:
      name: {{ .Values.schemaHistory.database.auth.existingSecret }}
      key: password
{{- else }}
  value: {{ .Values.schemaHistory.database.password }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Common labels
*/}}

{{- define "common.labels" -}}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}