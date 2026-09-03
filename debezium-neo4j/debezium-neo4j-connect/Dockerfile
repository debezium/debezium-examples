ARG DEBEZIUM_VERSION
FROM quay.io/debezium/connect:${DEBEZIUM_VERSION}

# This image only adds the official Neo4j Kafka Connect sink connector.
ARG NEO4J_CONNECTOR_VERSION=5.5.2

ENV NEO4J_CONNECTOR_DIR=$KAFKA_CONNECT_PLUGINS_DIR/neo4j-kafka-connector

USER root
RUN microdnf -y install unzip && \
    mkdir -p $NEO4J_CONNECTOR_DIR && cd $NEO4J_CONNECTOR_DIR && \
    curl -sSL -o neo4j-connector.zip \
      "https://github.com/neo4j/neo4j-kafka-connector/releases/download/${NEO4J_CONNECTOR_VERSION}/neo4j-kafka-connect-neo4j-${NEO4J_CONNECTOR_VERSION}.zip" && \
    unzip -o neo4j-connector.zip && rm -f neo4j-connector.zip && \
    microdnf -y remove unzip && microdnf clean all
USER kafka
