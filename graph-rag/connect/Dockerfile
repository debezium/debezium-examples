ARG DEBEZIUM_VERSION

# Builder stage: resolve the Debezium AI embeddings Ollama module and all of its runtime
# dependencies (the embeddings core + LangChain4j) into a single directory. They are placed in one
# Kafka Connect plugin directory so the FieldToEmbedding SMT and the OllamaModelFactory it discovers
# via SPI share a classloader.
FROM maven:3.9-eclipse-temurin-21 AS ai-embeddings
ARG DEBEZIUM_AI_VERSION
WORKDIR /build
RUN printf '%s\n' \
      '<project><modelVersion>4.0.0</modelVersion>' \
      '<groupId>io.debezium.examples</groupId><artifactId>ai-embeddings-deps</artifactId><version>1</version>' \
      '<dependencies><dependency>' \
      "<groupId>io.debezium</groupId><artifactId>debezium-ai-embeddings-ollama</artifactId><version>${DEBEZIUM_AI_VERSION}</version>" \
      '</dependency></dependencies></project>' > pom.xml && \
    mvn -q -B dependency:copy-dependencies -DoutputDirectory=/plugin -DincludeScope=runtime

FROM quay.io/debezium/connect:${DEBEZIUM_VERSION}

# Adds the official Neo4j Kafka Connect sink connector and the Debezium AI embeddings module.
ARG NEO4J_CONNECTOR_VERSION=5.5.2

ENV NEO4J_CONNECTOR_DIR=$KAFKA_CONNECT_PLUGINS_DIR/neo4j-kafka-connector

USER root
RUN microdnf -y install unzip && \
    mkdir -p $NEO4J_CONNECTOR_DIR && cd $NEO4J_CONNECTOR_DIR && \
    curl -sSL -o neo4j-connector.zip \
      "https://github.com/neo4j/neo4j-kafka-connector/releases/download/${NEO4J_CONNECTOR_VERSION}/neo4j-kafka-connect-neo4j-${NEO4J_CONNECTOR_VERSION}.zip" && \
    unzip -o neo4j-connector.zip && rm -f neo4j-connector.zip && \
    microdnf -y remove unzip && microdnf clean all

# The FieldToEmbedding loads its embedding-model factory
# (the Ollama provider) via Java's ServiceLoader, which uses the configuring connector's classloader.
# The jars must therefore live in the Debezium PostgreSQL connector's own plugin directory (sharing
# its classloader).
COPY --from=ai-embeddings /plugin $KAFKA_CONNECT_PLUGINS_DIR/debezium-connector-postgres/

USER kafka
