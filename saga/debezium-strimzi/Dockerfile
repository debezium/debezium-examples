FROM quay.io/strimzi/kafka:0.30.0-kafka-3.1.0
ENV KAFKA_CONNECT_PLUGIN_PATH=/tmp/connect-plugins

RUN mkdir $KAFKA_CONNECT_PLUGIN_PATH &&\
    cd $KAFKA_CONNECT_PLUGIN_PATH &&\
    curl -sfSL https://repo1.maven.org/maven2/io/debezium/debezium-connector-postgres/2.0.0.CR1/debezium-connector-postgres-2.0.0.CR1-plugin.tar.gz | tar xz
