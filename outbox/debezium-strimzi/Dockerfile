#ARG DEBEZIUM_VERSION=2.3
#
#FROM quay.io/debezium/connect:$DEBEZIUM_VERSION
#
#ARG OTEL_VERSION=1.23.0
#
#ENV KAFKA_CONNECT_SHARE_JAVA_PATH=/kafka/libs
#COPY "debezium-connector-postgres-2.4.0-SNAPSHOT-plugin.tar.gz" /
#COPY "debezium-interceptor-2.4.0-SNAPSHOT.jar" $KAFKA_CONNECT_SHARE_JAVA_PATH
#USER root
#
#RUN curl -L -o /opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_VERSION}/opentelemetry-javaagent.jar
#RUN curl -L -o ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-api-${OTEL_VERSION}.jar https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-api/${OTEL_VERSION}/opentelemetry-api-${OTEL_VERSION}.jar
#RUN curl -L -o ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-context-${OTEL_VERSION}.jar https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-context/${OTEL_VERSION}/opentelemetry-context-${OTEL_VERSION}.jar
#RUN curl -L -o ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-instrumentation-api-${OTEL_VERSION}.jar https://repo1.maven.org/maven2/io/opentelemetry/instrumentation/opentelemetry-instrumentation-api/${OTEL_VERSION}/opentelemetry-instrumentation-api-${OTEL_VERSION}.jar
#RUN curl -L -o ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-instrumentation-api-semconv-${OTEL_VERSION}-alpha.jar https://repo1.maven.org/maven2/io/opentelemetry/instrumentation/opentelemetry-instrumentation-api-semconv/${OTEL_VERSION}-alpha/opentelemetry-instrumentation-api-semconv-${OTEL_VERSION}-alpha.jar
#RUN curl -L -o ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-kafka-clients-2.6-${OTEL_VERSION}-alpha.jar https://repo1.maven.org/maven2/io/opentelemetry/instrumentation/opentelemetry-kafka-clients-2.6/${OTEL_VERSION}-alpha/opentelemetry-kafka-clients-2.6-${OTEL_VERSION}-alpha.jar
#RUN curl -L -o ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-kafka-clients-common-${OTEL_VERSION}-alpha.jar https://repo1.maven.org/maven2/io/opentelemetry/instrumentation/opentelemetry-kafka-clients-common/${OTEL_VERSION}-alpha/opentelemetry-kafka-clients-common-${OTEL_VERSION}-alpha.jar
#RUN curl -L -o ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-semconv-${OTEL_VERSION}-alpha.jar https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-semconv/${OTEL_VERSION}-alpha/opentelemetry-semconv-${OTEL_VERSION}-alpha.jar
#RUN chmod 666 ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-api-${OTEL_VERSION}.jar
#RUN chmod 666 ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-context-${OTEL_VERSION}.jar
#RUN chmod 666 ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-instrumentation-api-${OTEL_VERSION}.jar
#RUN chmod 666 ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-instrumentation-api-semconv-${OTEL_VERSION}-alpha.jar
#RUN chmod 666 ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-kafka-clients-2.6-${OTEL_VERSION}-alpha.jar
#RUN chmod 666 ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-kafka-clients-common-${OTEL_VERSION}-alpha.jar
#RUN chmod 666 ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-semconv-${OTEL_VERSION}-alpha.jar
#RUN chmod 666 $KAFKA_CONNECT_SHARE_JAVA_PATH/debezium-interceptor-2.4.0-SNAPSHOT.jar
#
#RUN cd /kafka/connect && \
#    rm -r debezium-connector-db2 && \
#    rm -r debezium-connector-jdbc && \
#    rm -r debezium-connector-mongodb && \
#    rm -r debezium-connector-mysql && \
#    rm -r debezium-connector-oracle && \
#    rm -r debezium-connector-postgres && \
#    rm -r debezium-connector-spanner && \
#    rm -r debezium-connector-sqlserver && \
#    rm -r debezium-connector-vitess && \
#    tar xzvf /debezium-connector-postgres-2.4.0-SNAPSHOT-plugin.tar.gz
#USER 1001
#
#ENV KAFKA_OPTS="-javaagent:/opentelemetry-javaagent.jar"

# Dockerfile optimisé
ARG DEBEZIUM_VERSION=2.3


FROM quay.io/debezium/connect:$DEBEZIUM_VERSION

ARG OTEL_VERSION=1.23.0
ENV KAFKA_CONNECT_SHARE_JAVA_PATH=/kafka/libs
ENV KAFKA_OPTS="-javaagent:/opentelemetry-javaagent.jar"

#TODO change to curl
COPY "debezium-interceptor-2.4.0-SNAPSHOT.jar" $KAFKA_CONNECT_SHARE_JAVA_PATH

WORKDIR /kafka/connect
#TODO change to curl
COPY "debezium-connector-postgres-2.4.0-SNAPSHOT-plugin.tar.gz" .

USER root

#TODO move to container image
RUN set -eux; \
    curl -sSL -o /opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.23.0/opentelemetry-javaagent.jar; \
    curl -sSL -o ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-api-1.23.1.jar https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-api/1.23.1/opentelemetry-api-1.23.1.jar; \
    curl -sSL -o ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-context-1.23.1.jar https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-context/1.23.1/opentelemetry-context-1.23.1.jar; \
    curl -sSL -o ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-instrumentation-api-${OTEL_VERSION}.jar https://repo1.maven.org/maven2/io/opentelemetry/instrumentation/opentelemetry-instrumentation-api/${OTEL_VERSION}/opentelemetry-instrumentation-api-${OTEL_VERSION}.jar; \
    curl -sSL -o ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-instrumentation-api-semconv-${OTEL_VERSION}-alpha.jar https://repo1.maven.org/maven2/io/opentelemetry/instrumentation/opentelemetry-instrumentation-api-semconv/${OTEL_VERSION}-alpha/opentelemetry-instrumentation-api-semconv-${OTEL_VERSION}-alpha.jar; \
    curl -sSL -o ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-kafka-clients-2.6-${OTEL_VERSION}-alpha.jar https://repo1.maven.org/maven2/io/opentelemetry/instrumentation/opentelemetry-kafka-clients-2.6/${OTEL_VERSION}-alpha/opentelemetry-kafka-clients-2.6-${OTEL_VERSION}-alpha.jar; \
    curl -sSL -o ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-kafka-clients-common-${OTEL_VERSION}-alpha.jar https://repo1.maven.org/maven2/io/opentelemetry/instrumentation/opentelemetry-kafka-clients-common/${OTEL_VERSION}-alpha/opentelemetry-kafka-clients-common-${OTEL_VERSION}-alpha.jar; \
    curl -sSL -o ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-semconv-1.23.1-alpha.jar https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-semconv/1.23.1-alpha/opentelemetry-semconv-1.23.1-alpha.jar; \
    chmod 666 ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-api-1.23.1.jar ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-context-1.23.1.jar ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-instrumentation-api-${OTEL_VERSION}.jar ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-instrumentation-api-semconv-${OTEL_VERSION}-alpha.jar ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-kafka-clients-2.6-${OTEL_VERSION}-alpha.jar ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-kafka-clients-common-${OTEL_VERSION}-alpha.jar ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-semconv-1.23.1-alpha.jar $KAFKA_CONNECT_SHARE_JAVA_PATH/debezium-interceptor-2.4.0-SNAPSHOT.jar;

#TODO change to curl
RUN set -eux; \
    rm -r debezium-connector-{db2,jdbc,mongodb,mysql,oracle,postgres,spanner,sqlserver,vitess}; \
    tar xzvf debezium-connector-postgres-2.4.0-SNAPSHOT-plugin.tar.gz

USER 1001