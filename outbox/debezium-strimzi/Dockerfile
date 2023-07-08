ARG STRIMZI_VERSION=latest-kafka-3.4.0
FROM quay.io/strimzi/kafka:${STRIMZI_VERSION}

ARG DEBEZIUM_CONNECTOR_VERSION=2.1.3.Final

ENV KAFKA_CONNECT_PLUGIN_PATH=/tmp/connect-plugins/
ENV KAFKA_CONNECT_SHARE_JAVA_PATH=/opt/kafka/libs
COPY "debezium-connector-postgres-2.4.0-SNAPSHOT-plugin.tar.gz" /
COPY "debezium-interceptor-otel-2.4.0-SNAPSHOT.jar" $KAFKA_CONNECT_SHARE_JAVA_PATH
USER root

RUN set -eux; \
    rm ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry*; \
    curl -sSL -o /opentelemetry-javaagent-1.23.0.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.23.0/opentelemetry-javaagent.jar; \
    curl -sSL -o ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-api-1.23.1.jar https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-api/1.23.1/opentelemetry-api-1.23.1.jar; \
    curl -sSL -o ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-context-1.23.1.jar https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-context/1.23.1/opentelemetry-context-1.23.1.jar; \
    curl -sSL -o ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-instrumentation-api-1.23.0.jar https://repo1.maven.org/maven2/io/opentelemetry/instrumentation/opentelemetry-instrumentation-api/1.23.0/opentelemetry-instrumentation-api-1.23.0.jar; \
    curl -sSL -o ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-instrumentation-api-semconv-1.23.0-alpha.jar https://repo1.maven.org/maven2/io/opentelemetry/instrumentation/opentelemetry-instrumentation-api-semconv/1.23.0-alpha/opentelemetry-instrumentation-api-semconv-1.23.0-alpha.jar; \
    curl -sSL -o ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-kafka-clients-2.6-1.23.0-alpha.jar https://repo1.maven.org/maven2/io/opentelemetry/instrumentation/opentelemetry-kafka-clients-2.6/1.23.0-alpha/opentelemetry-kafka-clients-2.6-1.23.0-alpha.jar; \
    curl -sSL -o ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-kafka-clients-common-1.23.0-alpha.jar https://repo1.maven.org/maven2/io/opentelemetry/instrumentation/opentelemetry-kafka-clients-common/1.23.0-alpha/opentelemetry-kafka-clients-common-1.23.0-alpha.jar; \
    curl -sSL -o ${KAFKA_CONNECT_SHARE_JAVA_PATH}/opentelemetry-semconv-1.23.1-alpha.jar https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-semconv/1.23.1-alpha/opentelemetry-semconv-1.23.1-alpha.jar; \
USER 1001

RUN mkdir $KAFKA_CONNECT_PLUGIN_PATH &&\
    cd $KAFKA_CONNECT_PLUGIN_PATH &&\
    tar xzvf /debezium-connector-postgres-2.4.0-SNAPSHOT-plugin.tar.gz

ENV KAFKA_OPTS="-javaagent:/opt/opentelemetry-javaagent-1.23.0.jar"