ARG GRAFANA_VERSION
FROM grafana/grafana:${GRAFANA_VERSION}

COPY dashboard.yml /etc/grafana/provisioning/dashboards
COPY datasource.yml /etc/grafana/provisioning/datasources
COPY debezium-dashboard.json /var/lib/grafana/dashboards/debezium-dashboard.json
COPY activity-monitoring-dashboard.json /var/lib/grafana/dashboards/activity-monitoring-dashboard.json
COPY alerting.yaml /etc/grafana/provisioning/alerting/alerting.yaml
COPY grafana.ini /etc/grafana/grafana.ini
