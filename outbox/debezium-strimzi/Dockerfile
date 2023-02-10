ARG STRIMZI_VERSION=latest-kafka-3.3.1
FROM quay.io/strimzi/kafka:${STRIMZI_VERSION}

ARG DEBEZIUM_CONNECTOR_VERSION=2.0.0.Final
ENV KAFKA_CONNECT_PLUGIN_PATH=/tmp/connect-plugins

RUN mkdir $KAFKA_CONNECT_PLUGIN_PATH &&\
    cd $KAFKA_CONNECT_PLUGIN_PATH &&\
    curl -sfSL https://repo1.maven.org/maven2/io/debezium/debezium-connector-postgres/${DEBEZIUM_CONNECTOR_VERSION}/debezium-connector-postgres-${DEBEZIUM_CONNECTOR_VERSION}-plugin.tar.gz | tar xz
