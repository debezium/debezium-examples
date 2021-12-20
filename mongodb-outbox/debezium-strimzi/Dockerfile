FROM quay.io/strimzi/kafka:0.26.0-kafka-3.0.0
ENV KAFKA_CONNECT_PLUGIN_PATH=/tmp/connect-plugins

RUN mkdir $KAFKA_CONNECT_PLUGIN_PATH &&\
    cd $KAFKA_CONNECT_PLUGIN_PATH &&\
    curl -sfSL https://repo1.maven.org/maven2/io/debezium/debezium-connector-mongodb/1.8.0.Final/debezium-connector-mongodb-1.8.0.Final-plugin.tar.gz | tar xz
