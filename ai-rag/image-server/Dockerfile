FROM quay.io/debezium/server:nightly AS builder

USER root
RUN microdnf -y install gzip && \
    microdnf clean all

USER jboss
RUN mkdir /tmp/ai && \
   curl -kL 'https://s01.oss.sonatype.org/service/local/artifact/maven/redirect?r=snapshots&g=io.debezium&a=debezium-ai-embeddings-ollama&v=3.2.0-SNAPSHOT&c=embeddings&e=tar.gz' | tar xvzf - -C /tmp/ai --strip-components=1

FROM quay.io/debezium/server:nightly

USER root
RUN microdnf clean all

USER jboss
COPY --from=builder /tmp/ai $SERVER_HOME/lib

