ARG STRIMZI_VERSION=latest-kafka-3.4.0
FROM quay.io/strimzi/kafka:${STRIMZI_VERSION}

ARG DEBEZIUM_CONNECTOR_VERSION=2.4.0.Alpha1

ENV KAFKA_CONNECT_PLUGIN_PATH=/tmp/connect-plugins/
ENV KAFKA_CONNECT_LIBS=/opt/kafka/libs
COPY "debezium-connector-postgres-2.4.0-SNAPSHOT-plugin.tar.gz" /
COPY "debezium-interceptor-2.4.0-SNAPSHOT.jar" $KAFKA_CONNECT_LIBS

RUN mkdir $KAFKA_CONNECT_PLUGIN_PATH &&\
    cd $KAFKA_CONNECT_PLUGIN_PATH &&\
    tar xzvf /debezium-connector-postgres-2.4.0-SNAPSHOT-plugin.tar.gz