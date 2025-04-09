FROM quay.io/debezium/server:3.2-ai-rag AS builder

USER root
RUN microdnf -y install gzip && \
    microdnf clean all

USER jboss
COPY debezium-ai-embeddings-ollama-3.2.0-SNAPSHOT-embeddings.tar.gz /tmp/debezium-ai-embeddings.tar.gz
RUN mkdir /tmp/ai && \
    tar xvzf /tmp/debezium-ai-embeddings.tar.gz -C /tmp/ai --strip-components=1
  #  curl -k https://repo1.maven.org/maven2/io/debezium/debezium-ai-embeddings-ollama/3.1.0.Final/debezium-ai-embeddings-ollama-3.1.0.Final-embeddings.tar.gz | tar xvzf - -C /tmp/ai --strip-components=1

FROM quay.io/debezium/server:3.2-ai-rag

USER root
RUN microdnf clean all

USER jboss
COPY --from=builder /tmp/ai $SERVER_HOME/lib

