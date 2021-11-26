FROM quay.io/strimzi/kafka:0.26.0-kafka-3.0.0
ENV KAFKA_CONNECT_PLUGIN_PATH=/tmp/connect-plugins

RUN mkdir $KAFKA_CONNECT_PLUGIN_PATH &&\
    cd $KAFKA_CONNECT_PLUGIN_PATH &&\
    curl -sfSL https://oss.sonatype.org/content/repositories/snapshots/io/debezium/debezium-connector-mongodb/1.8.0-SNAPSHOT/debezium-connector-mongodb-1.8.0-20211121.120228-187-plugin.tar.gz | tar xz