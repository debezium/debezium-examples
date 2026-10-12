ARG DEBEZIUM_VERSION
FROM quay.io/debezium/connect:${DEBEZIUM_VERSION}

# The Debezium connect image already bundles the Debezium JDBC sink connector
# (io.debezium.connector.jdbc.JdbcSinkConnector) and the PostgreSQL JDBC driver, and — on the
# nightly tag — the Neo4jDebeziumConverter SMT. This image only adds the official Neo4j Kafka
# Connect connector, whose archive ships the CDC *source* connector used here.
#
# `unzip` is already present in the base image, so we use it directly rather than installing it with
# microdnf (which would depend on the Fedora package mirrors being reachable at build time).
ARG NEO4J_CONNECTOR_VERSION=5.5.2

ENV NEO4J_CONNECTOR_DIR=$KAFKA_CONNECT_PLUGINS_DIR/neo4j-kafka-connector

USER root
RUN mkdir -p $NEO4J_CONNECTOR_DIR && cd $NEO4J_CONNECTOR_DIR && \
    curl -sSL -o neo4j-connector.zip \
      "https://github.com/neo4j/neo4j-kafka-connector/releases/download/${NEO4J_CONNECTOR_VERSION}/neo4j-kafka-connect-neo4j-${NEO4J_CONNECTOR_VERSION}.zip" && \
    unzip -o neo4j-connector.zip && rm -f neo4j-connector.zip
USER kafka
