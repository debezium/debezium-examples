FROM quay.io/debezium/server:3.2.0.Alpha1 AS builder

USER root
RUN microdnf -y install gzip && \
    microdnf clean all

USER jboss
RUN mkdir /tmp/ai && \
   curl -kL 'https://repo1.maven.org/maven2/io/debezium/debezium-ai-embeddings-ollama/3.2.0.Alpha1/debezium-ai-embeddings-ollama-3.2.0.Alpha1-embeddings.tar.gz' | tar xvzf - -C /tmp/ai --strip-components=1

FROM quay.io/debezium/server:3.2.0.Alpha1

USER root
RUN microdnf clean all

USER jboss
COPY --from=builder /tmp/ai $SERVER_HOME/lib

