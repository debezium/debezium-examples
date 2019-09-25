ARG DEBEZIUM_VERSION=0.10

FROM debezium/connect:$DEBEZIUM_VERSION

ARG POSTGRES_VERSION=42.2.8
ARG KAFKA_JDBC_VERSION=5.3.1

# Fetch and deploy PostgreSQL JDBC driver
RUN curl -sO http://central.maven.org/maven2/org/postgresql/postgresql/$POSTGRES_VERSION/postgresql-$POSTGRES_VERSION.jar
RUN mv postgresql-$POSTGRES_VERSION.jar /kafka/libs

# Fetch and deploy Kafka Connect JDBC
ENV KAFKA_CONNECT_JDBC_DIR=$KAFKA_CONNECT_PLUGINS_DIR/kafka-connect-jdbc
RUN mkdir $KAFKA_CONNECT_JDBC_DIR

RUN curl -sO http://packages.confluent.io/maven/io/confluent/kafka-connect-jdbc/$KAFKA_JDBC_VERSION/kafka-connect-jdbc-$KAFKA_JDBC_VERSION.jar
RUN mv kafka-connect-jdbc-$KAFKA_JDBC_VERSION.jar $KAFKA_CONNECT_JDBC_DIR

