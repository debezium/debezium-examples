ARG DEBEZIUM_VERSION=0.9

FROM debezium/connect:$DEBEZIUM_VERSION

# Deploy PostgreSQL JDBC Driver
COPY postgresql-42.1.4.jar /kafka/libs

# Deploy Kafka Connect JDBC
ENV KAFKA_CONNECT_JDBC_DIR=$KAFKA_CONNECT_PLUGINS_DIR/kafka-connect-jdbc
RUN mkdir $KAFKA_CONNECT_JDBC_DIR
COPY kafka-connect-jdbc-3.3.0.jar $KAFKA_CONNECT_JDBC_DIR
