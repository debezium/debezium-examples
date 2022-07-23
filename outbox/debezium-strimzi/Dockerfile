ARG STRIMZI_VERSION=0.30.0-kafka-3.1.0
FROM quay.io/strimzi/kafka:${STRIMZI_VERSION}

ARG DEBEZIUM_CONNECTOR_VERSION=1.9.5.Final
ENV KAFKA_CONNECT_PLUGIN_PATH=/tmp/connect-plugins

RUN mkdir $KAFKA_CONNECT_PLUGIN_PATH &&\
    cd $KAFKA_CONNECT_PLUGIN_PATH &&\
    curl -sfSL https://repo1.maven.org/maven2/io/debezium/debezium-connector-postgres/${DEBEZIUM_CONNECTOR_VERSION}/debezium-connector-postgres-${DEBEZIUM_CONNECTOR_VERSION}-plugin.tar.gz | tar xz
