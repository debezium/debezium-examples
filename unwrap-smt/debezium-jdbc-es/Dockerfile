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
RUN curl -sSL -o $KAFKA_CONNECT_PLUGINS_DIR/es-connector.zip "https://api.hub.confluent.io/api/plugins/confluentinc/kafka-connect-elasticsearch/versions/${KAFKA_ELASTICSEARCH_VERSION}/archive" && \
    unzip -d $KAFKA_CONNECT_PLUGINS_DIR $KAFKA_CONNECT_PLUGINS_DIR/es-connector.zip && \
    rm -f $KAFKA_CONNECT_PLUGINS_DIR/es-connector.zip

USER kafka

