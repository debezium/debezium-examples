ARG PROMETHEUS_VERSION
FROM prom/prometheus:${PROMETHEUS_VERSION}

RUN sed -i -e "s/'localhost:9090'/'localhost:9090','connect:8080'/" /etc/prometheus/prometheus.yml
