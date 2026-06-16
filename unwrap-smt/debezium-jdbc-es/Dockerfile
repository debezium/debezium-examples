ARG DEBEZIUM_VERSION
FROM quay.io/debezium/connect:${DEBEZIUM_VERSION}
ENV KAFKA_CONNECT_JDBC_DIR=$KAFKA_CONNECT_PLUGINS_DIR/kafka-connect-jdbc \
    KAFKA_CONNECT_ES_DIR=$KAFKA_CONNECT_PLUGINS_DIR/kafka-connect-elasticsearch

ARG POSTGRES_VERSION=42.5.1
ARG KAFKA_JDBC_VERSION=5.3.2
ARG KAFKA_ELASTICSEARCH_VERSION=15.0.1

# Deploy PostgreSQL JDBC Driver
RUN cd /kafka/libs && curl -sO https://jdbc.postgresql.org/download/postgresql-$POSTGRES_VERSION.jar

# Deploy Kafka Connect JDBC
RUN mkdir $KAFKA_CONNECT_JDBC_DIR && cd $KAFKA_CONNECT_JDBC_DIR &&\
	curl -sO https://packages.confluent.io/maven/io/confluent/kafka-connect-jdbc/$KAFKA_JDBC_VERSION/kafka-connect-jdbc-$KAFKA_JDBC_VERSION.jar

# Deploy Confluent Elasticsearch sink connector
USER root
RUN curl -sO https://client.hub.confluent.io/confluent-hub-client-latest.tar.gz && \
    tar -xzf confluent-hub-client-latest.tar.gz -C /tmp && \
    touch /tmp/worker.properties && \
    /tmp/bin/confluent-hub install --no-prompt --component-dir $KAFKA_CONNECT_PLUGINS_DIR --worker-configs /tmp/worker.properties confluentinc/kafka-connect-elasticsearch:${KAFKA_ELASTICSEARCH_VERSION} && \
    rm -rf /tmp/bin /tmp/share /tmp/etc /tmp/libexec confluent-hub-client-latest.tar.gz /tmp/worker.properties
USER kafka
